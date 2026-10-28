package com.ravibhaiya.quizzen.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.ravibhaiya.quizzen.domain.VocabularyEntry
import com.ravibhaiya.quizzen.domain.VocabularyQuizRules
import com.ravibhaiya.quizzen.domain.VocabularyRules
import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException

/**
 * Repository contract for the Vocabulary database.
 *
 * Cloud Firestore is the single source of truth. **Every write (save, merge, delete) needs internet**: it is sent to
 * the server first and only after the server confirmed it do the list and the local copy change. Without internet a
 * write fails right away with [NoInternetException] and nothing is saved or queued, so "success" always means "it is
 * in the database". Reading works offline from the local copy of the last server list.
 */
interface VocabularyRepository {
    val entries: Flow<List<VocabularyEntry>>
    val mistakeWords: Flow<Set<String>>
    val syncError: Flow<String?>
    suspend fun saveEntry(entry: VocabularyEntry): Result<VocabularyEntry>
    suspend fun saveEntries(entries: List<VocabularyEntry>): Result<List<VocabularyEntry>>
    suspend fun mergeIntoExisting(incoming: List<VocabularyEntry>): Result<VocabularyRules.MergeSummary>
    suspend fun deleteEntry(id: String): Result<Unit>
    suspend fun deleteEntries(ids: Set<String>): Result<Unit> {
        for (id in ids) {
            val res = deleteEntry(id)
            if (res.isFailure) return res
        }
        return Result.success(Unit)
    }
    suspend fun recordMistakeWords(wordKeys: Set<String>)
    suspend fun removeMistakeWords(wordKeys: Set<String>)
    fun clearSyncError()
}

/** Thrown by [VocabularyRepository.saveEntries] when a save would create a second entry for a main word. */
class DuplicateWordException(val words: List<String>) :
    IllegalArgumentException("Duplicate main word(s): ${words.joinToString(", ")}")

/** A write was refused because the phone is offline (or the server did not answer). Nothing was saved. */
class NoInternetException(
    message: String = "No internet connection. Nothing was saved. Connect and try again.",
) : IOException(message)

/**
 * Production implementation backed by Cloud Firestore (`vocabulary` collection).
 *
 * - **Reads**: a snapshot listener keeps [entries] equal to the server list. The server list always replaces what the
 *   app knew before (so a word deleted on another device disappears here too); the only exception is an empty answer
 *   that came from Firestore's own cache (first start without internet), which is ignored. Every server list is also
 *   written to a local JSON file so the words can be shown and quizzed offline.
 * - **Writes**: online only, each one a Firestore transaction that is awaited (see [inCloud]). A merge reads the
 *   current server copy of the word inside the transaction, so two devices merging into the same word cannot erase
 *   each other's synonyms. Nothing is ever queued for later.
 * - Nothing is seeded: no starter words and no test data are written to the database. The built-in starter words only
 *   exist as a read-only fallback inside the quiz (`VocabularyQuizRules`).
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
    private val mergeMutex = Mutex()
    private val cacheFile: File
        get() = File(appContext.filesDir, LOCAL_CACHE_FILENAME)
    private val mistakeFile: File
        get() = File(appContext.filesDir, LOCAL_MISTAKES_FILENAME)

    private val firestore: FirebaseFirestore? = runCatching {
        FirebaseFirestore.getInstance()
    }.onFailure { e ->
        Log.w(TAG, "FirebaseFirestore unavailable, vocabulary can only be read from the local copy", e)
    }.getOrNull()

    /** The last list the server gave us (or the local copy of it before the first answer). */
    private val knownEntries = MutableStateFlow<List<VocabularyEntry>>(emptyList())
    private val _mistakeWords = MutableStateFlow<Set<String>>(emptySet())
    override val mistakeWords: StateFlow<Set<String>> = _mistakeWords.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    override val syncError: StateFlow<String?> = _syncError.asStateFlow()

    override fun clearSyncError() {
        _syncError.value = null
    }

    init {
        scope.launch { _mistakeWords.value = readLocalMistakes() }
    }

    override val entries: StateFlow<List<VocabularyEntry>> = callbackFlow {
        // Show the copy from the last time the app was online right away; the server list replaces it.
        if (knownEntries.value.isEmpty()) knownEntries.value = readLocalCache()
        val localJob = launch { knownEntries.collect { trySend(it) } }

        val db = firestore
        if (db == null) {
            awaitClose { localJob.cancel() }
            return@callbackFlow
        }

        val registration: ListenerRegistration = db.collection(COLLECTION_VOCABULARY)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Firestore snapshot listener error: ${error.message}", error)
                    _syncError.value = error.localizedMessage ?: "Cloud sync error"
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
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
                // An empty answer from Firestore's own cache means "this device has not heard from the server yet"
                // (first start without internet), not "the database is empty": keep what is shown.
                if (remoteEntries.isEmpty() && snapshot.metadata.isFromCache) return@addSnapshotListener
                _syncError.value = null
                applyServerList(remoteEntries)
            }

        awaitClose {
            localJob.cancel()
            registration.remove()
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
            val sanitizedList = entries.map { entry ->
                val clean = entry.sanitized()
                require(clean.wordDetails.word.isNotBlank()) { "Main word cannot be empty" }
                clean
            }

            // One entry per main word: refuse a save that would create a second one (callers merge after approval).
            val known = knownEntries.value
            val wordCheck = VocabularyRules.checkMainWords(sanitizedList, known)
            if (wordCheck.clashing.isNotEmpty() || wordCheck.mergedRepeats > 0) {
                val names = wordCheck.skippedExisting.ifEmpty {
                    VocabularyRules.findDuplicateMainWords(sanitizedList).ifEmpty {
                        listOf("repeated words in the same save")
                    }
                }
                return@withContext Result.failure(DuplicateWordException(names))
            }

            val preparedList = sanitizedList.mapIndexed { index, clean ->
                val id = clean.id.ifBlank { VocabularyRules.documentIdForWord(clean.wordDetails.word) }
                val created = if (clean.createdAt > 0L) clean.createdAt else (now + index)
                clean.copy(
                    id = id,
                    createdAt = created,
                    updatedAt = now + index,
                )
            }

            // A word that is new to this device must still not exist on the server (another device may have just
            // added it); an edit of a known word replaces it.
            val knownIds = known.map { it.id }.toSet()
            val createOnlyIds = preparedList.map { it.id }.filterNot { it in knownIds }.toSet()

            val outcome = inCloud { db -> commitEntries(db, preparedList, createOnlyIds) }
            val alreadyThere = outcome.getOrElse { return@withContext Result.failure(it) }
            if (alreadyThere.isNotEmpty()) return@withContext Result.failure(DuplicateWordException(alreadyThere))

            applyConfirmed(preparedList)
            Result.success(preparedList)
        }

    override suspend fun mergeIntoExisting(
        incoming: List<VocabularyEntry>,
    ): Result<VocabularyRules.MergeSummary> = mergeMutex.withLock {
        withContext(Dispatchers.IO) {
            val current = knownEntries.value
            val byId = current.filter { it.id.isNotBlank() }.associateBy { it.id }
            val byKey = LinkedHashMap<String, VocabularyEntry>().apply {
                current.forEach { putIfAbsent(VocabularyRules.wordKey(it.wordDetails.word), it) }
            }
            // Which saved word does each incoming entry belong to? (several incoming entries may share one word)
            val perWord = LinkedHashMap<String, MutableList<VocabularyEntry>>()
            for (entry in incoming) {
                val target = (if (entry.id.isNotBlank()) byId[entry.id] else null)
                    ?: byKey[VocabularyRules.wordKey(entry.wordDetails.word)]
                    ?: continue
                if (target.id.isBlank()) continue
                perWord.getOrPut(target.id) { mutableListOf() }.add(entry)
            }
            if (perWord.isEmpty()) {
                return@withContext Result.success(VocabularyRules.MergeSummary(0, 0, 0, 0, false, 0))
            }

            val now = System.currentTimeMillis()
            val outcome = inCloud { db -> mergeOnServer(db, perWord, now) }
            val results = outcome.getOrElse { return@withContext Result.failure(it) }

            val saved = results.mapNotNull { it.saved }
            if (saved.isNotEmpty()) applyConfirmed(saved)
            Result.success(
                VocabularyRules.MergeSummary(
                    wordsUpdated = saved.size,
                    addedSynonyms = results.sumOf { it.addedSynonyms },
                    addedAntonyms = results.sumOf { it.addedAntonyms },
                    skipped = results.sumOf { it.skipped },
                    limitReached = results.any { it.limitReached },
                    enrichedRelations = results.sumOf { it.enrichedRelations },
                ),
            )
        }
    }

    override suspend fun deleteEntry(id: String): Result<Unit> =
        if (id.isBlank()) Result.success(Unit) else deleteEntries(setOf(id))

    override suspend fun deleteEntries(ids: Set<String>): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanIds = ids.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        if (cleanIds.isEmpty()) return@withContext Result.success(Unit)

        val outcome = inCloud { db ->
            val col = db.collection(COLLECTION_VOCABULARY)
            for (chunk in cleanIds.chunked(BATCH_CHUNK_SIZE)) {
                db.runTransaction { tx ->
                    for (id in chunk) tx.delete(col.document(id))
                    chunk.size
                }.await()
            }
            Unit
        }
        outcome.getOrElse { return@withContext Result.failure(it) }

        // The server confirmed the delete: now the list, the local copy and the mistake words follow.
        val deletedEntries = knownEntries.value.filter { it.id in cleanIds }
        val remaining = knownEntries.value.filterNot { it.id in cleanIds }
        knownEntries.value = remaining
        writeLocalCache(remaining)

        val mistakeKeysToRemove = buildSet {
            deletedEntries.forEach { addAll(VocabularyQuizRules.entryMistakeKeys(it)) }
            cleanIds.forEach { add(VocabularyQuizRules.mistakeKey(it)) }
        }
        removeMistakeWords(mistakeKeysToRemove)
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

    // ---- cloud helpers -------------------------------------------------------------------------------------------

    /** What one merge transaction did to one saved word (`saved` is null when nothing had to be written). */
    private data class ServerMerge(
        val saved: VocabularyEntry?,
        val addedSynonyms: Int = 0,
        val addedAntonyms: Int = 0,
        val skipped: Int = 0,
        val limitReached: Boolean = false,
        val enrichedRelations: Int = 0,
    )

    /**
     * Runs one cloud write. It fails at once without internet and gives up after [CLOUD_TIMEOUT_MS]. [block] must use
     * transactions (which Firestore never queues while offline) and await them, so returning normally means that the
     * server has the change.
     */
    private suspend fun <T : Any> inCloud(block: suspend (FirebaseFirestore) -> T): Result<T> {
        val db = firestore ?: return Result.failure(IOException(CLOUD_UNAVAILABLE_MESSAGE))
        if (!isOnline()) return Result.failure(NoInternetException())
        return try {
            val value = withTimeoutOrNull(CLOUD_TIMEOUT_MS) { block(db) }
            if (value == null) Result.failure(IOException(CLOUD_TIMEOUT_MESSAGE)) else Result.success(value)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Cloud write failed: ${e.message}", e)
            Result.failure(cloudFailure(e))
        }
    }

    private fun cloudFailure(e: Exception): Throwable {
        if (e is FirebaseFirestoreException) {
            return when (e.code) {
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
                -> NoInternetException()
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    IOException("The database refused this change (permission denied). Nothing was saved.")
                else -> IOException("Could not save to the cloud: ${e.code.name}. Nothing was saved.")
            }
        }
        return IOException("Could not save to the cloud: ${e.localizedMessage ?: "unknown error"}. Nothing was saved.")
    }

    private fun isOnline(): Boolean {
        val manager = appContext.getSystemService(ConnectivityManager::class.java) ?: return true
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Writes [prepared] in transactions of at most [BATCH_CHUNK_SIZE] documents (Firestore's limit is 500). Words in
     * [createOnlyIds] are refused when a document with that id already exists on the server; the refused words are
     * returned (and nothing from that chunk is written).
     */
    private suspend fun commitEntries(
        db: FirebaseFirestore,
        prepared: List<VocabularyEntry>,
        createOnlyIds: Set<String>,
    ): List<String> {
        val col = db.collection(COLLECTION_VOCABULARY)
        for (chunk in prepared.chunked(BATCH_CHUNK_SIZE)) {
            val taken: List<String> = db.runTransaction { tx ->
                // All reads first, then the writes (Firestore transactions require this order).
                val clashes = chunk
                    .filter { it.id in createOnlyIds && tx.get(col.document(it.id)).exists() }
                    .map { it.wordDetails.word }
                if (clashes.isEmpty()) {
                    for (entry in chunk) {
                        tx.set(col.document(entry.id), VocabularyFirestoreMapper.entryToMap(entry))
                    }
                }
                clashes
            }.await()
            if (taken.isNotEmpty()) return taken
        }
        return emptyList()
    }

    /** One transaction per word: read the word as it is on the server now, add what is new, write it back. */
    private suspend fun mergeOnServer(
        db: FirebaseFirestore,
        perWord: Map<String, List<VocabularyEntry>>,
        now: Long,
    ): List<ServerMerge> {
        val col = db.collection(COLLECTION_VOCABULARY)
        val results = mutableListOf<ServerMerge>()
        for ((id, incomingList) in perWord) {
            val ref = col.document(id)
            val result: ServerMerge = db.runTransaction { tx ->
                val snapshot = tx.get(ref)
                val onServer = if (snapshot.exists()) {
                    VocabularyFirestoreMapper.docToEntry(snapshot.id, snapshot.data)
                } else {
                    null
                }
                if (onServer == null) {
                    // Deleted on another device in the meantime: nothing to merge into.
                    ServerMerge(saved = null)
                } else {
                    var merged: VocabularyEntry = onServer
                    var syn = 0
                    var ant = 0
                    var skipped = 0
                    var enriched = 0
                    var limit = false
                    var changed = false
                    for (incoming in incomingList) {
                        val step = VocabularyRules.mergeRelations(merged, incoming)
                        merged = step.entry
                        syn += step.addedSynonyms
                        ant += step.addedAntonyms
                        skipped += step.skipped
                        enriched += step.enrichedRelations
                        limit = limit || step.limitReached
                        changed = changed || step.changed
                    }
                    val toSave = if (changed) merged.copy(updatedAt = now) else null
                    if (toSave != null) tx.set(ref, VocabularyFirestoreMapper.entryToMap(toSave))
                    ServerMerge(toSave, syn, ant, skipped, limit, enriched)
                }
            }.await()
            results += result
        }
        return results
    }

    // ---- local copy ----------------------------------------------------------------------------------------------

    /** The server list is the truth: it replaces everything the app knew and is written to the local copy. */
    private fun applyServerList(remote: List<VocabularyEntry>) {
        val sorted = remote.sortedByDescending { it.updatedAt }
        knownEntries.value = sorted
        scope.launch { writeLocalCache(sorted) }
    }

    /** Puts words the server just confirmed into the list at once (the snapshot listener repeats the same data). */
    private suspend fun applyConfirmed(saved: List<VocabularyEntry>) {
        val ids = saved.map { it.id }.toSet()
        val updated = (saved + knownEntries.value.filterNot { it.id in ids }).sortedByDescending { it.updatedAt }
        knownEntries.value = updated
        writeLocalCache(updated)
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
        private const val BATCH_CHUNK_SIZE = 400
        private const val CLOUD_TIMEOUT_MS = 20_000L
        private const val CLOUD_UNAVAILABLE_MESSAGE = "The cloud database is not available. Nothing was saved."
        private const val CLOUD_TIMEOUT_MESSAGE =
            "The server did not answer in time. Check your internet and the word list before trying again."
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
