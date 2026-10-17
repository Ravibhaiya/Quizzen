package com.ravibhaiya.quizzen.data

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.ravibhaiya.quizzen.domain.VocabularyEntry
import com.ravibhaiya.quizzen.domain.WordDetails
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

interface VocabularyRepository {
    val entries: Flow<List<VocabularyEntry>>
    val syncError: Flow<String?>
    suspend fun saveEntry(entry: VocabularyEntry): Result<VocabularyEntry>
    suspend fun saveEntries(entries: List<VocabularyEntry>): Result<List<VocabularyEntry>>
    suspend fun deleteEntry(id: String): Result<Unit>
    fun clearSyncError()
}

/**
 * Cloud Firestore + local offline cache repository for Vocabulary words, synonyms, and antonyms.
 *
 * Documents are stored in the top-level `vocabulary` collection with a future-proof, versioned
 * schema (`schemaVersion = 1`, `category = "vocabulary"`, normalized search/index fields
 * `wordLowercase`, `synonymWords`, `antonymWords`, nested `wordDetails`, `synonyms`, `antonyms`,
 * `createdAt`, and `updatedAt`) and verified after writes so any Firebase storage or fetch
 * failures surface immediately to the UI.
 */
class FirestoreVocabularyRepository(context: Context) : VocabularyRepository {

    private val appContext = context.applicationContext
    private val dataStore = appContext.quizzenDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _entries = MutableStateFlow<List<VocabularyEntry>>(emptyList())
    override val entries: Flow<List<VocabularyEntry>> = _entries.asStateFlow()

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
            val cached = loadLocalCache()
            if (cached.isNotEmpty() && _entries.value.isEmpty()) {
                _entries.value = sortEntries(cached)
            }
            observeFirestore()
            syncInitialWithFirestore(cached)
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
                        docToEntry(doc.id, doc.data)
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

    private suspend fun syncInitialWithFirestore(localCached: List<VocabularyEntry>) {
        val db = firestore ?: return
        runCatching {
            val snapshot = withTimeout(FIREBASE_TIMEOUT_MS) {
                db.collection(COLLECTION_VOCABULARY).get().await()
            }
            val remoteEntries = snapshot.documents.mapNotNull { doc ->
                docToEntry(doc.id, doc.data)
            }
            val remoteById = remoteEntries.associateBy { it.id }

            // Push any locally cached entries that aren't in Firestore yet
            for (local in localCached) {
                if (local.id !in pendingDeletes && local.id !in remoteById) {
                    runCatching {
                        withTimeout(FIREBASE_TIMEOUT_MS) {
                            db.collection(COLLECTION_VOCABULARY)
                                .document(local.id)
                                .set(entryToMap(local), SetOptions.merge())
                                .await()
                        }
                    }.onFailure { e ->
                        Log.w(TAG, "Failed to backfill local word '${local.id}' to Firebase", e)
                        _syncError.value = formatFirebaseError("Failed to sync word \"${local.wordDetails.word}\" to Firebase", e)
                    }
                }
            }

            applyRemoteEntries(
                remoteEntries = remoteEntries,
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

        val prepared = entries.mapIndexed { index, raw ->
            val sanitized = raw.sanitized()
            val matchedExisting = when {
                sanitized.id.isNotBlank() -> currentById[sanitized.id]
                else -> currentByWordLower[sanitized.wordDetails.word.lowercase()]
            }
            val id = when {
                sanitized.id.isNotBlank() -> sanitized.id
                matchedExisting != null -> matchedExisting.id
                else -> UUID.randomUUID().toString()
            }
            val timestamp = now + index
            val createdAt = when {
                sanitized.createdAt > 0L -> sanitized.createdAt
                matchedExisting != null && matchedExisting.createdAt > 0L -> matchedExisting.createdAt
                else -> timestamp
            }
            sanitized.copy(
                id = id,
                createdAt = createdAt,
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

        return try {
            withTimeout(FIREBASE_TIMEOUT_MS) {
                val batch = db.batch()
                for (item in prepared) {
                    val docRef = db.collection(COLLECTION_VOCABULARY).document(item.id)
                    batch.set(docRef, entryToMap(item))
                }
                batch.commit().await()

                // Verify that the saved documents can be fetched back and match the expected schema
                for (item in prepared) {
                    val docSnap = db.collection(COLLECTION_VOCABULARY)
                        .document(item.id)
                        .get()
                        .await()
                    val verified = docToEntry(docSnap.id, docSnap.data)
                    if (verified == null || ! verifiedMatches(item, verified)) {
                        throw IllegalStateException(
                            "Verification check failed while fetching saved word \"${item.wordDetails.word}\" from Firebase.",
                        )
                    }
                    pendingUpserts.remove(item.id, item)
                }
            }
            _syncError.value = null
            Result.success(prepared)
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

        val updatedList = _entries.value.filterNot { it.id == id }
        _entries.value = updatedList
        saveLocalCache(updatedList)

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

    private suspend fun loadLocalCache(): List<VocabularyEntry> {
        val raw = runCatching { dataStore.data.first()[LOCAL_CACHE_KEY] }.getOrNull() ?: return emptyList()
        return VocabularyJsonCodec.decodeList(raw)
    }

    private suspend fun saveLocalCache(list: List<VocabularyEntry>) {
        val encoded = VocabularyJsonCodec.encodeList(list)
        runCatching { dataStore.edit { it[LOCAL_CACHE_KEY] = encoded } }
    }

    private fun entryToMap(entry: VocabularyEntry): Map<String, Any> =
        VocabularyFirestoreMapper.entryToMap(entry)

    private fun docToEntry(id: String, data: Map<String, Any>?): VocabularyEntry? =
        VocabularyFirestoreMapper.docToEntry(id, data)

    private companion object {
        const val TAG = "VocabularyRepo"
        const val COLLECTION_VOCABULARY = "vocabulary"
        const val FIREBASE_TIMEOUT_MS = 12_000L

        val LOCAL_CACHE_KEY = stringPreferencesKey("vocabulary_entries_cache")
    }
}

internal object VocabularyFirestoreMapper {
    const val CATEGORY_VOCABULARY = "vocabulary"
    const val CURRENT_SCHEMA_VERSION = 1

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
        FIELD_WORD to entry.wordDetails.word,
        FIELD_WORD_LOWERCASE to entry.wordDetails.word.lowercase(),
        FIELD_MEANING_ENGLISH to entry.wordDetails.meaningEnglish,
        FIELD_MEANING_HINGLISH to entry.wordDetails.meaningHinglish,
        FIELD_EXAMPLE_SENTENCE to entry.wordDetails.exampleSentence,
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
        FIELD_WORD_LOWERCASE to details.word.lowercase(),
        FIELD_MEANING_ENGLISH to details.meaningEnglish,
        FIELD_MEANING_HINGLISH to details.meaningHinglish,
        FIELD_EXAMPLE_SENTENCE to details.exampleSentence,
    )

    fun docToEntry(id: String, data: Map<String, Any>?): VocabularyEntry? {
        if (data == null) return null
        val nestedDetails = mapToDetails(data[FIELD_WORD_DETAILS] as? Map<*, *>)
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
        val updatedAt = (data[FIELD_UPDATED_AT] as? Number)?.toLong() ?: 0L
        val createdAt = (data[FIELD_CREATED_AT] as? Number)?.toLong() ?: updatedAt
        return VocabularyEntry(
            id = id,
            wordDetails = wordDetails,
            synonyms = synonyms,
            antonyms = antonyms,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
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

internal object VocabularyJsonCodec {

    private const val ENTRY_SEP = "\n"
    private const val PART_SEP = "|"
    private const val ITEM_SEP = ";"
    private const val FIELD_SEP = ":"

    fun encodeList(entries: List<VocabularyEntry>): String =
        entries.joinToString(ENTRY_SEP) { entry ->
            listOf(
                b64Encode(entry.id),
                entry.createdAt.toString(),
                entry.updatedAt.toString(),
                encodeDetails(entry.wordDetails),
                entry.synonyms.joinToString(ITEM_SEP, transform = ::encodeDetails),
                entry.antonyms.joinToString(ITEM_SEP, transform = ::encodeDetails),
            ).joinToString(PART_SEP)
        }

    fun decodeList(raw: String): List<VocabularyEntry> {
        if (raw.isBlank()) return emptyList()
        return raw.split(ENTRY_SEP).mapNotNull { line ->
            val parts = line.split(PART_SEP)
            if (parts.size < 4) return@mapNotNull null
            val id = b64Decode(parts[0]).trim()
            val createdAt = parts[1].toLongOrNull() ?: 0L
            val updatedAt = parts[2].toLongOrNull() ?: createdAt
            val wordDetails = decodeDetails(parts[3]) ?: return@mapNotNull null
            if (id.isEmpty() || wordDetails.word.isEmpty()) return@mapNotNull null
            val synonyms = parts.getOrNull(4).orEmpty()
                .split(ITEM_SEP)
                .filter { it.isNotBlank() }
                .mapNotNull(::decodeDetails)
            val antonyms = parts.getOrNull(5).orEmpty()
                .split(ITEM_SEP)
                .filter { it.isNotBlank() }
                .mapNotNull(::decodeDetails)
            VocabularyEntry(
                id = id,
                wordDetails = wordDetails,
                synonyms = synonyms,
                antonyms = antonyms,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )
        }
    }

    private fun encodeDetails(details: WordDetails): String = listOf(
        b64Encode(details.word),
        b64Encode(details.meaningEnglish),
        b64Encode(details.meaningHinglish),
        b64Encode(details.exampleSentence),
    ).joinToString(FIELD_SEP)

    private fun decodeDetails(encoded: String): WordDetails? {
        val fields = encoded.split(FIELD_SEP)
        if (fields.isEmpty()) return null
        val word = b64Decode(fields.getOrNull(0).orEmpty()).trim()
        if (word.isEmpty()) return null
        return WordDetails(
            word = word,
            meaningEnglish = b64Decode(fields.getOrNull(1).orEmpty()).trim(),
            meaningHinglish = b64Decode(fields.getOrNull(2).orEmpty()).trim(),
            exampleSentence = b64Decode(fields.getOrNull(3).orEmpty()).trim(),
        )
    }

    private fun b64Encode(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))

    private fun b64Decode(encoded: String): String = runCatching {
        String(Base64.getUrlDecoder().decode(encoded), Charsets.UTF_8)
    }.getOrDefault("")
}

private var sharedRepositoryInstance: VocabularyRepository? = null

internal fun CreationExtras.vocabularyRepository(): VocabularyRepository {
    val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
    return synchronized(VocabularyRepository::class.java) {
        sharedRepositoryInstance ?: FirestoreVocabularyRepository(app).also {
            sharedRepositoryInstance = it
        }
    }
}
