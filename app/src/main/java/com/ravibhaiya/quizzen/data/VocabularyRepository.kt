package com.ravibhaiya.quizzen.data

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.ravibhaiya.quizzen.domain.VocabularyEntry
import com.ravibhaiya.quizzen.domain.VocabularyJsonFormat
import com.ravibhaiya.quizzen.domain.VocabularyQuizRules
import com.ravibhaiya.quizzen.domain.VocabularyRules
import com.ravibhaiya.quizzen.domain.WordDetails
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Base64
import java.util.Date
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

interface VocabularyRepository {
    val entries: Flow<List<VocabularyEntry>>
    val mistakeWords: Flow<Set<String>>
        get() = flowOf(emptySet())
    val syncError: Flow<String?>
    suspend fun saveEntry(entry: VocabularyEntry): Result<VocabularyEntry>
    suspend fun saveEntries(entries: List<VocabularyEntry>): Result<List<VocabularyEntry>>
    suspend fun deleteEntry(id: String): Result<Unit>
    suspend fun recordMistakeWords(wordKeys: Set<String>) = Unit
    suspend fun removeMistakeWords(wordKeys: Set<String>) = Unit
    fun clearSyncError()
}

/**
 * Cloud Firestore + local offline cache repository for Vocabulary words, synonyms, and antonyms.
 *
 * Documents are stored in the top-level `vocabulary` collection with a future-proof, versioned
 * schema (`schemaVersion = 2`, `category = "vocabulary"`, normalized search/index fields
 * `wordLowercase`, `synonymWords`, `antonymWords`, nested `wordDetails`, `synonyms`, `antonyms`,
 * `createdAt`, and `updatedAt`). Timestamps are written by the Firestore server (never the phone's
 * clock). Writes are split into batches of at most [BATCH_SIZE] documents and each batch is verified
 * (in parallel) so any Firebase storage or fetch failures surface immediately to the UI. The offline
 * copy is a versioned JSON file in app storage.
 */
class FirestoreVocabularyRepository(context: Context) : VocabularyRepository {

    private val appContext = context.applicationContext
    private val dataStore = appContext.quizzenDataStore
    private val cacheFile = File(appContext.filesDir, CACHE_FILE_NAME)
    private val mistakesFile = File(appContext.filesDir, MISTAKES_FILE_NAME)
    private val cacheMutex = Mutex()
    private val mistakesMutex = Mutex()
    @Volatile private var legacyCacheCleared = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _entries = MutableStateFlow<List<VocabularyEntry>>(emptyList())
    override val entries: Flow<List<VocabularyEntry>> = _entries.asStateFlow()

    private val _mistakeWords = MutableStateFlow<Set<String>>(loadMistakesFileSync())
    override val mistakeWords: Flow<Set<String>> = _mistakeWords.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    override val syncError: Flow<String?> = _syncError.asStateFlow()

    private val pendingUpserts = ConcurrentHashMap<String, VocabularyEntry>()
    private val pendingDeletes = ConcurrentHashMap.newKeySet<String>()

    private val firestore: FirebaseFirestore? = runCatching {
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            FirebaseApp.initializeApp(appContext)
        }
        FirebaseFirestore.getInstance().also { db ->
            runCatching {
                val settings = FirebaseFirestoreSettings.Builder(db.firestoreSettings)
                    .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                    .build()
                db.firestoreSettings = settings
            }
        }
    }.onFailure { e ->
        Log.w(TAG, "FirebaseFirestore initialization warning", e)
        _syncError.value = "Firebase initialization failed: ${e.localizedMessage ?: "Unknown error"}"
    }.getOrNull()

    init {
        scope.launch {
            syncInitialMistakes()
            val cached = loadLocalCache()
            val seedVersion = runCatching { dataStore.data.first()[SEED_VERSION_KEY] ?: 0 }.getOrDefault(0)
            val seedNeeded = seedVersion < CURRENT_SEED_VERSION
            val initialEntries = if (seedNeeded) {
                val cachedWordsLower = cached.map { it.wordDetails.word.lowercase() }.toSet()
                val now = System.currentTimeMillis()
                val missingSeeds = VocabularyQuizRules.FALLBACK_ENTRIES
                    .filter { it.wordDetails.word.lowercase() !in cachedWordsLower }
                    .mapIndexed { idx, entry ->
                        entry.copy(createdAt = now + idx, updatedAt = now + idx)
                    }
                val combined = sortEntries(cached + missingSeeds)
                saveLocalCache(combined)
                combined
            } else {
                sortEntries(cached)
            }
            if (initialEntries.isNotEmpty() && _entries.value.isEmpty()) {
                _entries.value = initialEntries
            }
            observeFirestore()
            syncInitialWithFirestore(initialEntries, seedNeeded = seedNeeded)
        }
    }

    override fun clearSyncError() {
        _syncError.value = null
    }

    private fun observeFirestore() {
        val db = firestore ?: return
        runCatching {
            db.collection(COLLECTION_VOCABULARY)
                .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Firestore snapshot listener error", error)
                        _syncError.value = formatFirebaseError("Failed to fetch data from Firebase", error)
                        return@addSnapshotListener
                    }
                    if (snapshot == null) return@addSnapshotListener
                    val remoteEntries = snapshot.documents.mapNotNull { doc ->
                        docToEntry(doc.id, doc.getData(ServerTimestampBehavior.ESTIMATE))
                    }
                    scope.launch {
                        applyRemoteEntries(
                            remoteEntries = remoteEntries,
                            isFromServer = !snapshot.metadata.isFromCache,
                        )
                    }
                }
        }.onFailure { e ->
            Log.w(TAG, "Failed to attach Firestore listener", e)
            _syncError.value = formatFirebaseError("Failed to listen for Firebase updates", e)
        }
    }

    private suspend fun syncInitialWithFirestore(
        localCached: List<VocabularyEntry>,
        seedNeeded: Boolean,
    ) {
        val db = firestore ?: return
        runCatching {
            val snapshot = withTimeout(FIREBASE_TIMEOUT_MS) {
                db.collection(COLLECTION_VOCABULARY).get().await()
            }
            val remoteEntries = snapshot.documents.mapNotNull { doc ->
                docToEntry(doc.id, doc.getData(ServerTimestampBehavior.ESTIMATE))
            }
            val remoteById = remoteEntries.associateBy { it.id }
            val remoteByWordLower = remoteEntries.associateBy { it.wordDetails.word.lowercase() }

            // Seed starter vocabulary words directly into Firebase when not yet seeded or if remote is empty
            if (seedNeeded || remoteEntries.isEmpty()) {
                val missingSeeds = VocabularyQuizRules.FALLBACK_ENTRIES.filter { seed ->
                    seed.id !in pendingDeletes &&
                        seed.id !in remoteById &&
                        seed.wordDetails.word.lowercase() !in remoteByWordLower
                }
                if (missingSeeds.isNotEmpty()) {
                    val seedResult = saveEntries(missingSeeds)
                    if (seedResult.isSuccess) {
                        runCatching { dataStore.edit { it[SEED_VERSION_KEY] = CURRENT_SEED_VERSION } }
                    }
                } else {
                    runCatching { dataStore.edit { it[SEED_VERSION_KEY] = CURRENT_SEED_VERSION } }
                }
            }

            // Push any locally cached entries that aren't in Firestore yet
            for (local in localCached) {
                if (local.id !in pendingDeletes &&
                    local.id !in remoteById &&
                    local.wordDetails.word.lowercase() !in remoteByWordLower
                ) {
                    runCatching {
                        withTimeout(FIREBASE_TIMEOUT_MS) {
                            db.collection(COLLECTION_VOCABULARY)
                                .document(local.id)
                                .set(entryToBackfillMap(local), SetOptions.merge())
                                .await()
                        }
                    }.onFailure { e ->
                        Log.w(TAG, "Failed to backfill local word '${local.id}' to Firebase", e)
                        _syncError.value = formatFirebaseError("Failed to sync word \"${local.wordDetails.word}\" to Firebase", e)
                    }
                }
            }

            val refreshedRemote = if (seedNeeded || remoteEntries.isEmpty()) {
                withTimeout(FIREBASE_TIMEOUT_MS) {
                    db.collection(COLLECTION_VOCABULARY).get().await()
                }.documents.mapNotNull { doc ->
                    docToEntry(doc.id, doc.getData(ServerTimestampBehavior.ESTIMATE))
                }
            } else {
                remoteEntries
            }

            applyRemoteEntries(
                remoteEntries = refreshedRemote,
                isFromServer = !snapshot.metadata.isFromCache,
            )
        }.onFailure { e ->
            if (e is CancellationException && e !is kotlinx.coroutines.TimeoutCancellationException) throw e
            Log.w(TAG, "Initial Firestore fetch warning", e)
            _syncError.value = formatFirebaseError("Could not fetch latest vocabulary from Firebase", e)
        }
    }

    private suspend fun applyRemoteEntries(
        remoteEntries: List<VocabularyEntry>,
        isFromServer: Boolean,
    ) {
        val localById = loadLocalCache().associateBy { it.id }
        val remoteById = remoteEntries
            .filterNot { it.id in pendingDeletes }
            .associateBy { it.id }

        val mergedMap = LinkedHashMap<String, VocabularyEntry>()
        if (isFromServer || remoteById.isNotEmpty()) {
            for ((id, remote) in remoteById) {
                val local = localById[id]
                mergedMap[id] = if (local != null && local.updatedAt > remote.updatedAt) local else remote
            }
            if (!isFromServer) {
                for ((id, local) in localById) {
                    if (id !in pendingDeletes && id !in mergedMap) {
                        mergedMap[id] = local
                    }
                }
            }
        } else {
            for ((id, local) in localById) {
                if (id !in pendingDeletes) {
                    mergedMap[id] = local
                }
            }
        }

        for ((id, pending) in pendingUpserts) {
            if (id !in pendingDeletes) {
                mergedMap[id] = pending
            }
        }

        val sorted = sortEntries(mergedMap.values)
        _entries.value = sorted
        saveLocalCache(sorted)
    }

    override suspend fun saveEntry(entry: VocabularyEntry): Result<VocabularyEntry> =
        saveEntries(listOf(entry)).map { it.first() }

    override suspend fun saveEntries(entries: List<VocabularyEntry>): Result<List<VocabularyEntry>> {
        if (entries.isEmpty()) return Result.success(emptyList())
        val db = firestore ?: run {
            val msg = "Firebase is not available. Could not save data to cloud."
            _syncError.value = msg
            return Result.failure(IllegalStateException(msg))
        }

        val now = System.currentTimeMillis()
        val currentById = _entries.value.associateBy { it.id }
        val currentByWordLower = _entries.value.associateBy { it.wordDetails.word.lowercase() }

        // One document per word: repeats inside the same import collapse into a single entry.
        val unique = VocabularyRules.mergeDuplicateWords(entries.map { it.sanitized() })

        val newIds = HashSet<String>()
        val prepared = unique.mapIndexed { index, sanitized ->
            val matchedExisting = sanitized.id.takeIf { it.isNotBlank() }?.let { currentById[it] }
                ?: currentByWordLower[sanitized.wordDetails.word.lowercase()]
            val id = when {
                matchedExisting != null -> matchedExisting.id
                sanitized.id.isNotBlank() -> sanitized.id
                else -> UUID.randomUUID().toString()
            }
            val timestamp = now + index
            val knownCreatedAt = when {
                sanitized.createdAt > 0L -> sanitized.createdAt
                matchedExisting != null && matchedExisting.createdAt > 0L -> matchedExisting.createdAt
                else -> 0L
            }
            if (knownCreatedAt == 0L) newIds.add(id)
            sanitized.copy(
                id = id,
                createdAt = if (knownCreatedAt > 0L) knownCreatedAt else timestamp,
                updatedAt = timestamp,
            )
        }

        val preparedIds = prepared.map { it.id }.toSet()
        for (item in prepared) {
            pendingDeletes.remove(item.id)
            pendingUpserts[item.id] = item
        }

        val optimisticList = sortEntries(_entries.value.filterNot { it.id in preparedIds } + prepared)
        _entries.value = optimisticList
        saveLocalCache(optimisticList)

        val confirmed = LinkedHashMap<String, VocabularyEntry>()
        return try {
            // Firestore allows at most 500 writes per batch, so large imports are saved in chunks,
            // each with its own timeout. Documents of a chunk are verified in parallel.
            for (chunk in prepared.chunked(BATCH_SIZE)) {
                val verifiedChunk = withTimeout(FIREBASE_TIMEOUT_MS) {
                    val batch = db.batch()
                    for (item in chunk) {
                        val docRef = db.collection(COLLECTION_VOCABULARY).document(item.id)
                        batch.set(docRef, entryToWriteMap(item, isNew = item.id in newIds))
                    }
                    batch.commit().await()

                    coroutineScope {
                        chunk.map { item ->
                            async {
                                val docSnap = db.collection(COLLECTION_VOCABULARY)
                                    .document(item.id)
                                    .get()
                                    .await()
                                val verified = docToEntry(
                                    docSnap.id,
                                    docSnap.getData(ServerTimestampBehavior.ESTIMATE),
                                )
                                if (verified == null || !verifiedMatches(item, verified)) {
                                    throw IllegalStateException(
                                        "Verification check failed while fetching saved word \"${item.wordDetails.word}\" from Firebase.",
                                    )
                                }
                                item to verified
                            }
                        }.awaitAll()
                    }
                }
                for ((item, verified) in verifiedChunk) {
                    pendingUpserts.remove(item.id, item)
                    // Adopt the server's timestamps so ordering and merging never depend on this phone's clock.
                    confirmed[item.id] = item.copy(
                        createdAt = verified.createdAt.takeIf { it > 0L } ?: item.createdAt,
                        updatedAt = verified.updatedAt.takeIf { it > 0L } ?: item.updatedAt,
                    )
                }
            }
            val confirmedList = sortEntries(_entries.value.map { confirmed[it.id] ?: it })
            _entries.value = confirmedList
            saveLocalCache(confirmedList)
            _syncError.value = null
            Result.success(prepared.map { confirmed[it.id] ?: it })
        } catch (e: CancellationException) {
            if (e !is kotlinx.coroutines.TimeoutCancellationException) throw e
            val msg = "Timed out while saving to Firebase. Please check your internet connection or Firebase rules."
            Log.w(TAG, msg, e)
            _syncError.value = msg
            Result.failure(IllegalStateException(msg, e))
        } catch (e: Exception) {
            val msg = formatFirebaseError("Failed to save vocabulary to Firebase", e)
            Log.w(TAG, msg, e)
            _syncError.value = msg
            Result.failure(IllegalStateException(msg, e))
        }
    }

    override suspend fun deleteEntry(id: String): Result<Unit> {
        val db = firestore ?: run {
            val msg = "Firebase is not available. Could not delete word from cloud."
            _syncError.value = msg
            return Result.failure(IllegalStateException(msg))
        }

        pendingUpserts.remove(id)
        pendingDeletes.add(id)

        val deletedEntry = _entries.value.firstOrNull { it.id == id }
        val updatedList = _entries.value.filterNot { it.id == id }
        _entries.value = updatedList
        saveLocalCache(updatedList)
        if (deletedEntry != null) {
            removeMistakeWords(VocabularyQuizRules.mistakeKeysForEntry(deletedEntry))
        }

        return try {
            withTimeout(FIREBASE_TIMEOUT_MS) {
                val docRef = db.collection(COLLECTION_VOCABULARY).document(id)
                docRef.delete().await()
                val checkSnap = docRef.get(Source.CACHE).await()
                if (checkSnap.exists()) {
                    throw IllegalStateException("Verification check failed: deleted word still exists in cache.")
                }
            }
            pendingDeletes.remove(id)
            _syncError.value = null
            Result.success(Unit)
        } catch (e: CancellationException) {
            if (e !is kotlinx.coroutines.TimeoutCancellationException) throw e
            val msg = "Timed out while deleting word from Firebase."
            Log.w(TAG, msg, e)
            _syncError.value = msg
            Result.failure(IllegalStateException(msg, e))
        } catch (e: Exception) {
            val msg = formatFirebaseError("Failed to delete word from Firebase", e)
            Log.w(TAG, msg, e)
            _syncError.value = msg
            Result.failure(IllegalStateException(msg, e))
        }
    }

    override suspend fun recordMistakeWords(wordKeys: Set<String>) {
        val normalized = wordKeys.map(VocabularyQuizRules::mistakeKey).filter { it.isNotEmpty() }.toSet()
        if (normalized.isEmpty()) return
        mistakesMutex.withLock {
            val updated = _mistakeWords.value + normalized
            _mistakeWords.value = updated
            persistMistakes(updated)
        }
    }

    override suspend fun removeMistakeWords(wordKeys: Set<String>) {
        val normalized = wordKeys.map(VocabularyQuizRules::mistakeKey).filter { it.isNotEmpty() }.toSet()
        if (normalized.isEmpty()) return
        mistakesMutex.withLock {
            val updated = _mistakeWords.value - normalized
            _mistakeWords.value = updated
            persistMistakes(updated)
        }
    }

    private fun loadMistakesFileSync(): Set<String> = runCatching {
        if (!mistakesFile.exists()) return@runCatching emptySet()
        mistakesFile.readLines(Charsets.UTF_8)
            .map(VocabularyQuizRules::mistakeKey)
            .filter { it.isNotEmpty() }
            .toSet()
    }.getOrDefault(emptySet())

    private suspend fun syncInitialMistakes() {
        mistakesMutex.withLock {
            val fromDataStore = runCatching {
                dataStore.data.first()[MISTAKE_WORDS_KEY].orEmpty()
                    .map(VocabularyQuizRules::mistakeKey)
                    .filter { it.isNotEmpty() }
                    .toSet()
            }.getOrDefault(emptySet())
            val merged = _mistakeWords.value + fromDataStore
            if (merged != _mistakeWords.value) {
                _mistakeWords.value = merged
            }
            if (merged.isNotEmpty()) {
                persistMistakes(merged)
            }
        }
    }

    private suspend fun persistMistakes(words: Set<String>) {
        withContext(Dispatchers.IO) {
            runCatching {
                dataStore.edit { prefs ->
                    prefs[MISTAKE_WORDS_KEY] = words
                }
            }.onFailure { Log.w(TAG, "Failed to write mistake words to DataStore", it) }

            runCatching {
                val tmp = File(mistakesFile.parentFile, "$MISTAKES_FILE_NAME.tmp")
                tmp.writeText(words.joinToString(separator = "\n"), Charsets.UTF_8)
                try {
                    Files.move(tmp.toPath(), mistakesFile.toPath(), StandardCopyOption.ATOMIC_MOVE)
                } catch (e: AtomicMoveNotSupportedException) {
                    Files.move(tmp.toPath(), mistakesFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
            }.onFailure { Log.w(TAG, "Failed to write mistake words file", it) }
        }
    }

    private fun verifiedMatches(expected: VocabularyEntry, actual: VocabularyEntry): Boolean =
        expected.id == actual.id &&
            expected.wordDetails == actual.wordDetails &&
            expected.synonyms == actual.synonyms &&
            expected.antonyms == actual.antonyms

    private fun formatFirebaseError(prefix: String, error: Throwable): String {
        val detail = error.localizedMessage?.takeIf { it.isNotBlank() } ?: error.javaClass.simpleName
        return "$prefix: $detail"
    }

    private fun sortEntries(entries: Collection<VocabularyEntry>): List<VocabularyEntry> =
        entries.sortedWith(
            compareByDescending<VocabularyEntry> { it.updatedAt }
                .thenBy { it.wordDetails.word.lowercase() },
        )

    private suspend fun loadLocalCache(): List<VocabularyEntry> = cacheMutex.withLock {
        withContext(Dispatchers.IO) {
            val fromFile = runCatching {
                if (cacheFile.exists()) cacheFile.readText(Charsets.UTF_8) else null
            }.getOrNull()
            if (fromFile != null) return@withContext VocabularyJsonCodec.decodeList(fromFile)
            // One-time migration: older versions kept the cache as a single DataStore string.
            val legacy = runCatching { dataStore.data.first()[LEGACY_CACHE_KEY] }.getOrNull()
                ?: return@withContext emptyList()
            VocabularyJsonCodec.decodeList(legacy)
        }
    }

    private suspend fun saveLocalCache(list: List<VocabularyEntry>) {
        cacheMutex.withLock {
            withContext(Dispatchers.IO) {
                val written = runCatching {
                    val tmp = File(cacheFile.parentFile, "$CACHE_FILE_NAME.tmp")
                    tmp.writeText(VocabularyJsonCodec.encodeList(list), Charsets.UTF_8)
                    try {
                        Files.move(tmp.toPath(), cacheFile.toPath(), StandardCopyOption.ATOMIC_MOVE)
                    } catch (e: AtomicMoveNotSupportedException) {
                        Files.move(tmp.toPath(), cacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    }
                }.onFailure { Log.w(TAG, "Failed to write vocabulary cache", it) }.isSuccess
                if (written && !legacyCacheCleared) {
                    runCatching { dataStore.edit { it.remove(LEGACY_CACHE_KEY) } }
                    legacyCacheCleared = true
                }
            }
        }
    }

    /**
     * Same as [entryToMap] but every stored timestamp is a Firestore [Timestamp] (never a plain number), so the
     * fields have one type across all documents. The server stamps `updatedAt` (and `createdAt` for new words);
     * an existing word keeps its stored creation time.
     */
    private fun entryToWriteMap(entry: VocabularyEntry, isNew: Boolean): Map<String, Any> {
        val map = VocabularyFirestoreMapper.entryToMap(entry).toMutableMap()
        map[VocabularyFirestoreMapper.FIELD_UPDATED_AT] = FieldValue.serverTimestamp()
        map[VocabularyFirestoreMapper.FIELD_CREATED_AT] =
            if (isNew) FieldValue.serverTimestamp() else Timestamp(Date(entry.createdAt))
        return map
    }

    /** Map for copying a local-only word to the server unchanged (keeps its own times, as Timestamps). */
    private fun entryToBackfillMap(entry: VocabularyEntry): Map<String, Any> {
        val map = VocabularyFirestoreMapper.entryToMap(entry).toMutableMap()
        map[VocabularyFirestoreMapper.FIELD_UPDATED_AT] = Timestamp(Date(entry.updatedAt))
        map[VocabularyFirestoreMapper.FIELD_CREATED_AT] = Timestamp(Date(entry.createdAt))
        return map
    }

    private fun docToEntry(id: String, data: Map<String, Any>?): VocabularyEntry? {
        val ignored = VocabularyFirestoreMapper.malformedRelationCount(data)
        if (ignored > 0) {
            Log.w(TAG, "Ignored $ignored malformed synonym/antonym item(s) in word document '$id'")
        }
        return VocabularyFirestoreMapper.docToEntry(id, data)
    }

    private companion object {
        const val TAG = "VocabularyRepo"
        const val COLLECTION_VOCABULARY = "vocabulary"
        const val FIREBASE_TIMEOUT_MS = 12_000L
        const val BATCH_SIZE = 400
        const val CACHE_FILE_NAME = "vocabulary_cache.json"
        const val MISTAKES_FILE_NAME = "vocabulary_mistakes.txt"
        const val CURRENT_SEED_VERSION = 1

        val LEGACY_CACHE_KEY = stringPreferencesKey("vocabulary_entries_cache")
        val SEED_VERSION_KEY = intPreferencesKey("vocabulary_firebase_seed_version")
        val MISTAKE_WORDS_KEY = stringSetPreferencesKey("vocabulary_mistake_words")
    }
}

internal object VocabularyFirestoreMapper {
    const val CATEGORY_VOCABULARY = "vocabulary"

    /**
     * 2 = word fields live only inside `wordDetails` (no duplicated top-level copies), per-item
     * `wordLowercase` removed, timestamps written by the server. Version 1 documents are still read.
     */
    const val CURRENT_SCHEMA_VERSION = 2

    const val FIELD_ID = "id"
    const val FIELD_SCHEMA_VERSION = "schemaVersion"
    const val FIELD_CATEGORY = "category"
    const val FIELD_WORD = "word"
    const val FIELD_WORD_LOWERCASE = "wordLowercase"
    const val FIELD_MEANING_ENGLISH = "meaningEnglish"
    const val FIELD_MEANING_HINGLISH = "meaningHinglish"
    const val FIELD_EXAMPLE_SENTENCE = "exampleSentence"
    const val FIELD_WORD_DETAILS = "wordDetails"
    const val FIELD_SYNONYMS = "synonyms"
    const val FIELD_ANTONYMS = "antonyms"
    const val FIELD_SYNONYM_WORDS = "synonymWords"
    const val FIELD_ANTONYM_WORDS = "antonymWords"
    const val FIELD_CREATED_AT = "createdAt"
    const val FIELD_UPDATED_AT = "updatedAt"

    fun entryToMap(entry: VocabularyEntry): Map<String, Any> = mapOf(
        FIELD_ID to entry.id,
        FIELD_SCHEMA_VERSION to CURRENT_SCHEMA_VERSION,
        FIELD_CATEGORY to CATEGORY_VOCABULARY,
        FIELD_WORD_LOWERCASE to entry.wordDetails.word.lowercase(),
        FIELD_WORD_DETAILS to detailsToMap(entry.wordDetails),
        FIELD_SYNONYMS to entry.synonyms.map(::detailsToMap),
        FIELD_ANTONYMS to entry.antonyms.map(::detailsToMap),
        FIELD_SYNONYM_WORDS to entry.synonyms.map { it.word.lowercase() },
        FIELD_ANTONYM_WORDS to entry.antonyms.map { it.word.lowercase() },
        FIELD_CREATED_AT to entry.createdAt,
        FIELD_UPDATED_AT to entry.updatedAt,
    )

    fun detailsToMap(details: WordDetails): Map<String, String> = mapOf(
        FIELD_WORD to details.word,
        FIELD_MEANING_ENGLISH to details.meaningEnglish,
        FIELD_MEANING_HINGLISH to details.meaningHinglish,
        FIELD_EXAMPLE_SENTENCE to details.exampleSentence,
    )

    fun docToEntry(id: String, data: Map<String, Any>?): VocabularyEntry? {
        if (data == null) return null
        val nestedDetails = mapToDetails(data[FIELD_WORD_DETAILS] as? Map<*, *>)
        // Version 1 documents also carried the same fields at the top level; fall back to them.
        val wordDetails = nestedDetails ?: run {
            val word = (data[FIELD_WORD] as? String).orEmpty().trim()
            if (word.isEmpty()) return null
            WordDetails(
                word = word,
                meaningEnglish = (data[FIELD_MEANING_ENGLISH] as? String).orEmpty().trim(),
                meaningHinglish = (data[FIELD_MEANING_HINGLISH] as? String).orEmpty().trim(),
                exampleSentence = (data[FIELD_EXAMPLE_SENTENCE] as? String).orEmpty().trim(),
            )
        }
        val synonyms = (data[FIELD_SYNONYMS] as? List<*>)
            .orEmpty()
            .mapNotNull { mapToDetails(it as? Map<*, *>) }
        val antonyms = (data[FIELD_ANTONYMS] as? List<*>)
            .orEmpty()
            .mapNotNull { mapToDetails(it as? Map<*, *>) }
        val updatedAt = toMillis(data[FIELD_UPDATED_AT])
        val createdAt = toMillis(data[FIELD_CREATED_AT]).takeIf { it > 0L } ?: updatedAt
        return VocabularyEntry(
            id = id,
            wordDetails = wordDetails,
            synonyms = synonyms,
            antonyms = antonyms,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    /** How many synonym/antonym items in a stored document cannot be read (and are therefore ignored). */
    fun malformedRelationCount(data: Map<String, Any>?): Int {
        if (data == null) return 0
        return listOf(FIELD_SYNONYMS, FIELD_ANTONYMS).sumOf { key ->
            (data[key] as? List<*>).orEmpty().count { mapToDetails(it as? Map<*, *>) == null }
        }
    }

    /** Timestamps are plain millisecond numbers (version 1 / local) or Firestore server [Timestamp]s. */
    private fun toMillis(value: Any?): Long = when (value) {
        is Number -> value.toLong()
        is Timestamp -> value.toDate().time
        else -> 0L
    }

    private fun mapToDetails(map: Map<*, *>?): WordDetails? {
        if (map == null) return null
        val word = (map[FIELD_WORD] as? String).orEmpty().trim()
        if (word.isEmpty()) return null
        return WordDetails(
            word = word,
            meaningEnglish = (map[FIELD_MEANING_ENGLISH] as? String).orEmpty().trim(),
            meaningHinglish = (map[FIELD_MEANING_HINGLISH] as? String).orEmpty().trim(),
            exampleSentence = (map[FIELD_EXAMPLE_SENTENCE] as? String).orEmpty().trim(),
        )
    }
}

/**
 * Offline cache format: `{"version":2,"entries":[{id, createdAt, updatedAt, wordDetails, synonyms, antonyms}]}`.
 * The pre-version-2 pipe/colon/Base64 text is still decoded so existing installs keep their cache.
 */
internal object VocabularyJsonCodec {

    const val CACHE_VERSION = 2

    fun encodeList(entries: List<VocabularyEntry>): String = buildString {
        append("{\"version\":").append(CACHE_VERSION).append(",\"entries\":[")
        entries.forEachIndexed { index, entry ->
            if (index > 0) append(',')
            append("{\"id\":").append(VocabularyJsonFormat.escapeJson(entry.id))
            append(",\"createdAt\":").append(entry.createdAt)
            append(",\"updatedAt\":").append(entry.updatedAt)
            append(",\"wordDetails\":").append(encodeDetails(entry.wordDetails))
            append(",\"synonyms\":").append(encodeDetailsList(entry.synonyms))
            append(",\"antonyms\":").append(encodeDetailsList(entry.antonyms))
            append('}')
        }
        append("]}")
    }

    fun decodeList(raw: String): List<VocabularyEntry> {
        val text = raw.trim()
        if (text.isEmpty()) return emptyList()
        if (!text.startsWith("{")) return decodeLegacy(text)
        val root = VocabularyJsonFormat.parseTree(text) as? Map<*, *> ?: return emptyList()
        val list = root["entries"] as? List<*> ?: return emptyList()
        return list.mapNotNull { decodeEntry(it as? Map<*, *>) }
    }

    private fun encodeDetails(details: WordDetails): String = buildString {
        append("{\"word\":").append(VocabularyJsonFormat.escapeJson(details.word))
        append(",\"meaningEnglish\":").append(VocabularyJsonFormat.escapeJson(details.meaningEnglish))
        append(",\"meaningHinglish\":").append(VocabularyJsonFormat.escapeJson(details.meaningHinglish))
        append(",\"exampleSentence\":").append(VocabularyJsonFormat.escapeJson(details.exampleSentence))
        append('}')
    }

    private fun encodeDetailsList(items: List<WordDetails>): String =
        items.joinToString(separator = ",", prefix = "[", postfix = "]", transform = ::encodeDetails)

    private fun decodeEntry(map: Map<*, *>?): VocabularyEntry? {
        if (map == null) return null
        val id = (map["id"] as? String).orEmpty().trim()
        val wordDetails = decodeDetails(map["wordDetails"] as? Map<*, *>) ?: return null
        if (id.isEmpty()) return null
        val updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: 0L
        val createdAt = (map["createdAt"] as? Number)?.toLong() ?: updatedAt
        return VocabularyEntry(
            id = id,
            wordDetails = wordDetails,
            synonyms = (map["synonyms"] as? List<*>).orEmpty().mapNotNull { decodeDetails(it as? Map<*, *>) },
            antonyms = (map["antonyms"] as? List<*>).orEmpty().mapNotNull { decodeDetails(it as? Map<*, *>) },
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    private fun decodeDetails(map: Map<*, *>?): WordDetails? {
        if (map == null) return null
        val word = (map["word"] as? String).orEmpty().trim()
        if (word.isEmpty()) return null
        return WordDetails(
            word = word,
            meaningEnglish = (map["meaningEnglish"] as? String).orEmpty().trim(),
            meaningHinglish = (map["meaningHinglish"] as? String).orEmpty().trim(),
            exampleSentence = (map["exampleSentence"] as? String).orEmpty().trim(),
        )
    }

    // ---- legacy (pre-version-2) text format, read-only ----

    private const val LEGACY_ENTRY_SEP = "\n"
    private const val LEGACY_PART_SEP = "|"
    private const val LEGACY_ITEM_SEP = ";"
    private const val LEGACY_FIELD_SEP = ":"

    private fun decodeLegacy(raw: String): List<VocabularyEntry> =
        raw.split(LEGACY_ENTRY_SEP).mapNotNull { line ->
            val parts = line.split(LEGACY_PART_SEP)
            if (parts.size < 4) return@mapNotNull null
            val id = b64Decode(parts[0]).trim()
            val createdAt = parts[1].toLongOrNull() ?: 0L
            val updatedAt = parts[2].toLongOrNull() ?: createdAt
            val wordDetails = decodeLegacyDetails(parts[3]) ?: return@mapNotNull null
            if (id.isEmpty()) return@mapNotNull null
            VocabularyEntry(
                id = id,
                wordDetails = wordDetails,
                synonyms = parts.getOrNull(4).orEmpty().split(LEGACY_ITEM_SEP)
                    .filter { it.isNotBlank() }.mapNotNull(::decodeLegacyDetails),
                antonyms = parts.getOrNull(5).orEmpty().split(LEGACY_ITEM_SEP)
                    .filter { it.isNotBlank() }.mapNotNull(::decodeLegacyDetails),
                createdAt = createdAt,
                updatedAt = updatedAt,
            )
        }

    private fun decodeLegacyDetails(encoded: String): WordDetails? {
        val fields = encoded.split(LEGACY_FIELD_SEP)
        val word = b64Decode(fields.getOrNull(0).orEmpty()).trim()
        if (word.isEmpty()) return null
        return WordDetails(
            word = word,
            meaningEnglish = b64Decode(fields.getOrNull(1).orEmpty()).trim(),
            meaningHinglish = b64Decode(fields.getOrNull(2).orEmpty()).trim(),
            exampleSentence = b64Decode(fields.getOrNull(3).orEmpty()).trim(),
        )
    }

    private fun b64Decode(encoded: String): String = runCatching {
        String(Base64.getUrlDecoder().decode(encoded), Charsets.UTF_8)
    }.getOrDefault("")
}

private var sharedRepositoryInstance: VocabularyRepository? = null

internal fun ensureVocabularyRepositoryInitialized(context: Context): VocabularyRepository {
    val appContext = context.applicationContext
    return synchronized(VocabularyRepository::class.java) {
        sharedRepositoryInstance ?: FirestoreVocabularyRepository(appContext).also {
            sharedRepositoryInstance = it
        }
    }
}

internal fun CreationExtras.vocabularyRepository(): VocabularyRepository {
    val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
    return ensureVocabularyRepositoryInitialized(app)
}
