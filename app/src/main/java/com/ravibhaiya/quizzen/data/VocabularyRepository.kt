package com.ravibhaiya.quizzen.data

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.ravibhaiya.quizzen.domain.VocabularyEntry
import com.ravibhaiya.quizzen.domain.VocabularyQuizRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Repository contract for managing Vocabulary words with real-time updates and cloud + local persistence.
 */
interface VocabularyRepository {
    val entries: Flow<List<VocabularyEntry>>
    val mistakeWords: Flow<Set<String>>
    val syncError: Flow<String?>
    suspend fun saveEntry(entry: VocabularyEntry): Result<VocabularyEntry>
    suspend fun saveEntries(entries: List<VocabularyEntry>): Result<List<VocabularyEntry>>
    suspend fun deleteEntry(id: String): Result<Unit>
    suspend fun recordMistakeWords(wordKeys: Set<String>)
    suspend fun removeMistakeWords(wordKeys: Set<String>)
    fun clearSyncError()
}

/**
 * Production implementation backed by Firebase Cloud Firestore (`vocabulary` collection) and a local JSON file
 * cache in `context.filesDir` so the user's words are always available immediately (even offline or if Firestore
 * security rules reject a write) and automatically synced with Firebase when online.
 *
 * Also persists locally saved mistake words (`vocabulary_mistakes_v1.txt`) so the Vocabulary quiz algorithm can
 * prioritize words the user answered wrong or timed out on until they answer all questions for that word right.
 */
class FirestoreVocabularyRepository(
    context: Context,
) : VocabularyRepository {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cacheMutex = Mutex()
    private val mistakeMutex = Mutex()
    private val cacheFile: File
        get() = File(appContext.filesDir, LOCAL_CACHE_FILENAME)
    private val mistakeFile: File
        get() = File(appContext.filesDir, LOCAL_MISTAKES_FILENAME)

    private val firestore: FirebaseFirestore? = runCatching {
        FirebaseFirestore.getInstance()
    }.onFailure { e ->
        Log.w(TAG, "FirebaseFirestore unavailable, falling back to local cache only", e)
    }.getOrNull()

    private val localFallbackFlow = MutableStateFlow<List<VocabularyEntry>>(emptyList())
    private val _mistakeWords = MutableStateFlow<Set<String>>(emptySet())
    override val mistakeWords: StateFlow<Set<String>> = _mistakeWords.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    override val syncError: StateFlow<String?> = _syncError.asStateFlow()

    override fun clearSyncError() {
        _syncError.value = null
    }

    init {
        scope.launch {
            val cached = readLocalCache()
            val mistakes = readLocalMistakes()
            _mistakeWords.value = mistakes
            if (cached.isNotEmpty()) {
                localFallbackFlow.value = cached
            } else {
                val seeded = VocabularyQuizRules.FALLBACK_ENTRIES
                writeLocalCache(seeded)
                localFallbackFlow.value = seeded
            }
        }
    }

    override val entries: StateFlow<List<VocabularyEntry>> = callbackFlow {
        val initialCache = readLocalCache().ifEmpty { VocabularyQuizRules.FALLBACK_ENTRIES }
        localFallbackFlow.value = initialCache
        trySend(initialCache)

        val db = firestore
        if (db == null) {
            val job = launch {
                localFallbackFlow.collect { trySend(it) }
            }
            awaitClose { job.cancel() }
            return@callbackFlow
        }

        val collection = db.collection(COLLECTION_VOCABULARY)
        var seededEmptyRemote = false

        var registration: ListenerRegistration? = null
        val localJob = launch {
            localFallbackFlow.collect { trySend(it) }
        }

        registration = collection
            .orderBy(FIELD_UPDATED_AT, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Firestore snapshot listener error: ${error.message}", error)
                    _syncError.value = error.localizedMessage ?: "Cloud sync error"
                    trySend(localFallbackFlow.value)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    var malformedRelations = 0
                    val remoteEntries = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data
                        malformedRelations += VocabularyFirestoreMapper.malformedRelationCount(data)
                        val parsed = VocabularyFirestoreMapper.docToEntry(doc.id, data)
                        if (parsed == null) {
                            Log.w(TAG, "Skipping malformed vocabulary document id=${doc.id}")
                        }
                        parsed
                    }
                    if (malformedRelations > 0) {
                        Log.w(TAG, "Ignored $malformedRelations malformed synonym/antonym item(s) across snapshot")
                    }
                    if (remoteEntries.isEmpty() && !seededEmptyRemote) {
                        seededEmptyRemote = true
                        val seedList = VocabularyQuizRules.FALLBACK_ENTRIES
                        localFallbackFlow.value = seedList
                        trySend(seedList)
                        scope.launch {
                            writeLocalCache(seedList)
                            runCatching {
                                val batch = db.batch()
                                for (seed in seedList) {
                                    val docRef = collection.document(seed.id)
                                    batch.set(docRef, VocabularyFirestoreMapper.entryToMap(seed))
                                }
                                batch.commit().await()
                            }.onFailure { e ->
                                Log.w(TAG, "Initial Firestore seed skipped: ${e.message}")
                            }
                        }
                    } else {
                        val merged = mergeRemoteAndLocal(remoteEntries, localFallbackFlow.value)
                        localFallbackFlow.value = merged
                        scope.launch { writeLocalCache(merged) }
                        trySend(merged)
                    }
                }
            }

        awaitClose {
            localJob.cancel()
            registration?.remove()
        }
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    override suspend fun saveEntry(entry: VocabularyEntry): Result<VocabularyEntry> =
        saveEntries(listOf(entry)).map { it.first() }

    override suspend fun saveEntries(entries: List<VocabularyEntry>): Result<List<VocabularyEntry>> =
        withContext(Dispatchers.IO) {
            if (entries.isEmpty()) return@withContext Result.success(emptyList())
            val now = System.currentTimeMillis()
            val preparedList = entries.mapIndexed { index, entry ->
                val clean = entry.sanitized()
                require(clean.wordDetails.word.isNotBlank()) { "Main word cannot be empty" }
                val id = clean.id.ifBlank { UUID.randomUUID().toString() }
                val created = if (clean.createdAt > 0L) clean.createdAt else (now + index)
                clean.copy(
                    id = id,
                    createdAt = created,
                    updatedAt = now + index,
                )
            }

            // 1. Update local cache immediately for instant UI responsiveness and offline safety.
            val newIds = preparedList.map { it.id }.toSet()
            val updatedLocal = buildList {
                addAll(preparedList.sortedByDescending { it.updatedAt })
                localFallbackFlow.value.forEach { existing ->
                    if (existing.id !in newIds) add(existing)
                }
            }.sortedByDescending { it.updatedAt }
            localFallbackFlow.value = updatedLocal
            writeLocalCache(updatedLocal)

            // 2. Sync to Firebase Firestore in a single batch when available.
            val db = firestore ?: return@withContext Result.success(preparedList)
            runCatching {
                val batch = db.batch()
                val col = db.collection(COLLECTION_VOCABULARY)
                for (prepared in preparedList) {
                    val docRef = col.document(prepared.id)
                    batch.set(docRef, VocabularyFirestoreMapper.entryToMap(prepared))
                }
                batch.commit().await()
                _syncError.value = null
            }.onFailure { e ->
                Log.w(TAG, "Firestore saveEntries failed (saved locally): ${e.message}", e)
                _syncError.value = e.localizedMessage ?: "Saved locally; cloud sync failed"
            }

            Result.success(preparedList)
        }

    override suspend fun deleteEntry(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (id.isBlank()) return@withContext Result.success(Unit)

        val deletedEntry = localFallbackFlow.value.firstOrNull { it.id == id }
        val updatedLocal = localFallbackFlow.value.filterNot { it.id == id }
        localFallbackFlow.value = updatedLocal
        writeLocalCache(updatedLocal)

        if (deletedEntry != null) {
            removeMistakeWords(VocabularyQuizRules.entryMistakeKeys(deletedEntry))
        } else {
            removeMistakeWords(setOf(VocabularyQuizRules.mistakeKey(id)))
        }

        val db = firestore ?: return@withContext Result.success(Unit)
        runCatching {
            db.collection(COLLECTION_VOCABULARY)
                .document(id)
                .delete()
                .await()
            _syncError.value = null
        }.onFailure { e ->
            Log.w(TAG, "Firestore deleteEntry failed (deleted locally): ${e.message}", e)
            _syncError.value = e.localizedMessage ?: "Deleted locally; cloud sync failed"
        }

        Result.success(Unit)
    }

    override suspend fun recordMistakeWords(wordKeys: Set<String>) {
        val cleaned = wordKeys.map(VocabularyQuizRules::mistakeKey).filter { it.isNotEmpty() }.toSet()
        if (cleaned.isEmpty()) return
        withContext(Dispatchers.IO) {
            mistakeMutex.withLock {
                val updated = _mistakeWords.value + cleaned
                _mistakeWords.value = updated
                writeLocalMistakesLocked(updated)
            }
        }
    }

    override suspend fun removeMistakeWords(wordKeys: Set<String>) {
        val cleaned = wordKeys.map(VocabularyQuizRules::mistakeKey).filter { it.isNotEmpty() }.toSet()
        if (cleaned.isEmpty()) return
        withContext(Dispatchers.IO) {
            mistakeMutex.withLock {
                val updated = _mistakeWords.value - cleaned
                if (updated != _mistakeWords.value) {
                    _mistakeWords.value = updated
                    writeLocalMistakesLocked(updated)
                }
            }
        }
    }

    private fun mergeRemoteAndLocal(
        remote: List<VocabularyEntry>,
        local: List<VocabularyEntry>,
    ): List<VocabularyEntry> {
        if (remote.isEmpty()) return local
        val byId = LinkedHashMap<String, VocabularyEntry>()
        remote.forEach { byId[it.id] = it }
        local.forEach { localEntry ->
            val existing = byId[localEntry.id]
            if (existing == null || localEntry.updatedAt > existing.updatedAt) {
                byId[localEntry.id] = localEntry
            }
        }
        return byId.values.sortedByDescending { it.updatedAt }
    }

    private suspend fun readLocalCache(): List<VocabularyEntry> = cacheMutex.withLock {
        runCatching {
            if (!cacheFile.exists()) return@runCatching emptyList()
            VocabularyJsonCodec.decodeList(cacheFile.readText(Charsets.UTF_8))
        }.getOrDefault(emptyList())
    }

    private suspend fun writeLocalCache(entries: List<VocabularyEntry>) = cacheMutex.withLock {
        runCatching {
            cacheFile.writeText(VocabularyJsonCodec.encodeList(entries), Charsets.UTF_8)
        }.onFailure { e ->
            Log.w(TAG, "Failed writing local vocabulary cache", e)
        }
    }

    private suspend fun readLocalMistakes(): Set<String> = mistakeMutex.withLock {
        runCatching {
            if (!mistakeFile.exists()) return@runCatching emptySet()
            mistakeFile.readLines(Charsets.UTF_8)
                .map(VocabularyQuizRules::mistakeKey)
                .filter { it.isNotEmpty() }
                .toSet()
        }.getOrDefault(emptySet())
    }

    private fun writeLocalMistakesLocked(keys: Set<String>) {
        runCatching {
            mistakeFile.writeText(keys.sorted().joinToString("\n"), Charsets.UTF_8)
        }.onFailure { e ->
            Log.w(TAG, "Failed writing local vocabulary mistake words", e)
        }
    }

    companion object {
        private const val TAG = "VocabularyRepo"
        const val COLLECTION_VOCABULARY = "vocabulary"
        private const val FIELD_UPDATED_AT = "updatedAt"
        private const val LOCAL_CACHE_FILENAME = "vocabulary_cache_v1.json"
        private const val LOCAL_MISTAKES_FILENAME = "vocabulary_mistakes_v1.txt"

        @Volatile
        private var instance: VocabularyRepository? = null

        fun getInstance(context: Context): VocabularyRepository =
            instance ?: synchronized(this) {
                instance ?: FirestoreVocabularyRepository(context.applicationContext).also { instance = it }
            }
    }
}

fun ensureVocabularyRepositoryInitialized(context: Context): VocabularyRepository =
    FirestoreVocabularyRepository.getInstance(context)

fun CreationExtras.vocabularyRepository(): VocabularyRepository =
    FirestoreVocabularyRepository.getInstance(checkNotNull(this[APPLICATION_KEY]))
