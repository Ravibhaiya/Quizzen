package com.ravibhaiya.quizzen.data

import android.content.Context
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.ravibhaiya.quizzen.domain.VocabularyDecisionFinding
import com.ravibhaiya.quizzen.domain.VocabularyDecisionRules
import com.ravibhaiya.quizzen.domain.VocabularyEntry
import com.ravibhaiya.quizzen.domain.VocabularyJsonFormat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Thrown when `gemini-decision-1` hits a temporary rate limit (`HTTP 429` / `50K TPM`), server overload,
 * or network timeout so the UI can place verification in **Pending** status and automatically re-verify
 * after [retrySeconds] (default 60 seconds). Any findings from batches that completed before the pause
 * are carried in [partialFindings].
 */
class DecisionPendingRetryException(
    message: String,
    val retrySeconds: Int = DEFAULT_RETRY_SECONDS,
    val partialFindings: List<VocabularyDecisionFinding> = emptyList(),
    cause: Throwable? = null,
) : IOException(message, cause) {
    companion object {
        const val DEFAULT_RETRY_SECONDS = 60
    }
}

/**
 * Evaluates vocabulary entries for exact/fuzzy duplicates (root/inflected forms, spelling variants, typos)
 * and synonym/antonym meaning conflicts using Google's `gemini-decision-1` model.
 */
interface VocabularyDecisionClient {
    val isConfigured: Boolean
        get() = true

    val apiKeyChanges: Flow<String>
        get() = emptyFlow()

    suspend fun analyze(
        entries: List<VocabularyEntry>,
        dismissedKeys: Set<String> = emptySet(),
    ): Result<List<VocabularyDecisionFinding>>
}

/**
 * Production implementation calling the Google Gemini Decisions API
 * (`POST https://generativelanguage.googleapis.com/v1beta/decisions`) with `model = "gemini-decision-1"`.
 *
 * Implements **Option B** incremental verification:
 * - Builds request plans and parses responses on [Dispatchers.IO] in batches of at most
 *   [VocabularyDecisionRules.BATCH_MAX_QUESTIONS] (`100`) questions.
 * - Persists verified word fingerprints (`entryId -> entryContentDigest`) and active findings on-device
 *   (`vocabulary_decision_state_v1.json`) after every completed batch so unchanged words are never
 *   re-checked across app restarts or 60-second rate-limit retries.
 */
class GeminiDecisionClient(
    private val settingsRepository: SettingsRepository? = null,
    context: Context? = null,
    private val apiKeyProvider: () -> String = { DataStoreSettingsRepository.effectiveGeminiApiKey() },
    private val requestExecutor: ((apiKey: String, requestJson: String) -> String)? = null,
) : VocabularyDecisionClient {

    private val appContext = context?.applicationContext
    private val stateMutex = Mutex()
    private var memorySnapshot: VocabularyDecisionRules.DecisionVerificationSnapshot? = null

    private val stateFile: File?
        get() = appContext?.filesDir?.let { File(it, LOCAL_STATE_FILENAME) }

    override val isConfigured: Boolean
        get() = DataStoreSettingsRepository.sanitizeApiKey(apiKeyProvider()).isNotEmpty()

    override val apiKeyChanges: Flow<String> =
        settingsRepository?.geminiApiKey?.map { stored ->
            val cleaned = DataStoreSettingsRepository.sanitizeApiKey(stored)
            cleaned.ifEmpty { DataStoreSettingsRepository.sanitizeApiKey(apiKeyProvider()) }
        } ?: emptyFlow()

    override suspend fun analyze(
        entries: List<VocabularyEntry>,
        dismissedKeys: Set<String>,
    ): Result<List<VocabularyDecisionFinding>> = withContext(Dispatchers.IO) {
        val storedKey = DataStoreSettingsRepository.sanitizeApiKey(
            settingsRepository?.geminiApiKey?.first().orEmpty(),
        )
        val apiKey = storedKey.ifEmpty {
            DataStoreSettingsRepository.sanitizeApiKey(apiKeyProvider())
        }

        if (apiKey.isEmpty()) {
            return@withContext Result.failure(IllegalStateException(MISSING_API_KEY_MESSAGE))
        }

        val currentDigests = entries
            .filter { it.id.isNotBlank() && it.wordDetails.word.isNotBlank() }
            .associate { it.id to VocabularyDecisionRules.entryContentDigest(it) }

        val initialState = stateMutex.withLock {
            val loaded = loadSnapshotLocked()
            val validVerifiedDigests = loaded.verifiedDigests.filter { (id, digest) ->
                currentDigests[id] == digest
            }
            val verifiedIds = validVerifiedDigests.keys
            val validFindings = loaded.findings.filter { finding ->
                finding.key !in dismissedKeys && when (finding) {
                    is VocabularyDecisionFinding.DuplicateOrVariant ->
                        finding.primaryId in verifiedIds && finding.duplicateId in verifiedIds
                    is VocabularyDecisionFinding.MeaningConflict ->
                        finding.entryId in verifiedIds
                }
            }
            val pruned = VocabularyDecisionRules.DecisionVerificationSnapshot(
                verifiedDigests = validVerifiedDigests,
                findings = validFindings,
            )
            if (pruned != loaded) {
                saveSnapshotLocked(pruned)
            }
            pruned
        }

        val batches = VocabularyDecisionRules.buildRequestBatches(
            entries = entries,
            dismissedKeys = dismissedKeys,
            verifiedEntryIds = initialState.verifiedDigests.keys,
            maxQuestionsPerBatch = VocabularyDecisionRules.BATCH_MAX_QUESTIONS,
        )

        if (batches.isEmpty()) {
            return@withContext Result.success(initialState.findings)
        }

        val workingDigests = LinkedHashMap(initialState.verifiedDigests)
        val findingsByKey = LinkedHashMap<String, VocabularyDecisionFinding>()
        for (existingFinding in initialState.findings) {
            findingsByKey[existingFinding.key] = existingFinding
        }

        for (batch in batches) {
            try {
                val responseBody = withTimeout(DECISION_TIMEOUT_MS) {
                    val custom = requestExecutor
                    if (custom != null) {
                        custom(apiKey, batch.requestJson)
                    } else {
                        postDecisionRequest(apiKey, batch.requestJson)
                    }
                }
                val batchFindings = VocabularyDecisionRules.parseResponse(responseBody, batch)
                for (finding in batchFindings) {
                    if (finding.key !in dismissedKeys) {
                        findingsByKey[finding.key] = finding
                    }
                }
                for (coveredId in batch.coveredEntryIds) {
                    val digest = currentDigests[coveredId]
                    if (digest != null) {
                        workingDigests[coveredId] = digest
                    }
                }
                stateMutex.withLock {
                    saveSnapshotLocked(
                        VocabularyDecisionRules.DecisionVerificationSnapshot(
                            verifiedDigests = LinkedHashMap(workingDigests),
                            findings = findingsByKey.values.toList(),
                        ),
                    )
                }
            } catch (e: CancellationException) {
                if (e is kotlinx.coroutines.TimeoutCancellationException) {
                    return@withContext Result.failure(
                        DecisionPendingRetryException(
                            message = "Gemini Decision 1 timed out. Verification is pending and will automatically retry in 60 seconds.",
                            partialFindings = findingsByKey.values.toList(),
                            cause = e,
                        ),
                    )
                } else {
                    throw e
                }
            } catch (e: DecisionPendingRetryException) {
                return@withContext Result.failure(
                    DecisionPendingRetryException(
                        message = e.message ?: "Verification is pending and will automatically retry in 60 seconds.",
                        retrySeconds = e.retrySeconds,
                        partialFindings = findingsByKey.values.toList(),
                        cause = e,
                    ),
                )
            } catch (e: Exception) {
                val wrapped = wrapDecisionError(e, findingsByKey.values.toList())
                return@withContext Result.failure(wrapped)
            }
        }

        Result.success(findingsByKey.values.toList())
    }

    /**
     * Verifies a candidate Gemini API key with a single lightweight `gemini-decision-1` request on [Dispatchers.IO]
     * (zero output tokens). Does not touch persisted verification state.
     */
    suspend fun verifyApiKey(rawApiKey: String): Result<Unit> = withContext(Dispatchers.IO) {
        val cleaned = DataStoreSettingsRepository.sanitizeApiKey(rawApiKey)
        if (cleaned.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException(MISSING_API_KEY_MESSAGE))
        }
        try {
            withTimeout(DECISION_TIMEOUT_MS) {
                val custom = requestExecutor
                if (custom != null) {
                    custom(cleaned, VERIFY_KEY_REQUEST_JSON)
                } else {
                    postDecisionRequest(cleaned, VERIFY_KEY_REQUEST_JSON)
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            if (e is kotlinx.coroutines.TimeoutCancellationException) {
                Result.failure(IOException("Connection timed out while verifying Gemini API key. Try again.", e))
            } else {
                throw e
            }
        } catch (e: Exception) {
            Result.failure(wrapDecisionError(e))
        }
    }

    private fun loadSnapshotLocked(): VocabularyDecisionRules.DecisionVerificationSnapshot {
        memorySnapshot?.let { return it }
        val file = stateFile
        val decoded = if (file != null && file.exists()) {
            runCatching {
                VocabularyDecisionRules.decodeVerificationSnapshot(file.readText(Charsets.UTF_8))
            }.getOrDefault(VocabularyDecisionRules.DecisionVerificationSnapshot())
        } else {
            VocabularyDecisionRules.DecisionVerificationSnapshot()
        }
        memorySnapshot = decoded
        return decoded
    }

    private fun saveSnapshotLocked(snapshot: VocabularyDecisionRules.DecisionVerificationSnapshot) {
        memorySnapshot = snapshot
        val file = stateFile ?: return
        runCatching {
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.writeText(VocabularyDecisionRules.encodeVerificationSnapshot(snapshot), Charsets.UTF_8)
            if (!tmp.renameTo(file)) {
                file.writeText(VocabularyDecisionRules.encodeVerificationSnapshot(snapshot), Charsets.UTF_8)
                tmp.delete()
            }
        }
    }

    private fun postDecisionRequest(apiKey: String, requestJson: String): String {
        val connection = (URL(DECISIONS_ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("x-goog-api-key", apiKey)
        }
        try {
            connection.outputStream.use { out ->
                out.write(requestJson.toByteArray(Charsets.UTF_8))
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code in 200..299) {
                return body
            }
            val detail = extractApiErrorMessage(body) ?: "HTTP $code"
            throw buildHttpDecisionException(code, detail)
        } finally {
            connection.disconnect()
        }
    }

    private fun buildHttpDecisionException(code: Int, detail: String): IOException {
        val lower = detail.lowercase()
        return when {
            code == 429 || "quota" in lower || "rate limit" in lower || "resource_exhausted" in lower || "tpm" in lower ->
                DecisionPendingRetryException(
                    message = "Gemini Decision 1 rate limit (50K TPM) reached. Verification is placed in pending status and will automatically re-verify in 60 seconds.",
                )
            code in 500..599 ->
                DecisionPendingRetryException(
                    message = "Gemini Decision 1 server is temporarily busy (HTTP $code). Verification is placed in pending status and will automatically re-verify in 60 seconds.",
                )
            code == 400 && ("api key not valid" in lower || "api_key_invalid" in lower) ->
                IOException("Invalid Gemini API key. Open Settings -> Gemini API Key, paste a valid Google AI Studio key, and try again.")
            code == 401 || code == 403 ->
                IOException("Gemini API key was rejected (HTTP $code). Open Settings -> Gemini API Key and verify your key has Gemini API access.")
            else ->
                IOException("Gemini Decision 1 request failed ($detail).")
        }
    }

    private fun wrapDecisionError(
        error: Exception,
        partialFindings: List<VocabularyDecisionFinding> = emptyList(),
    ): IOException {
        if (error is DecisionPendingRetryException) {
            return DecisionPendingRetryException(
                message = error.message ?: "Verification is pending and will automatically retry in 60 seconds.",
                retrySeconds = error.retrySeconds,
                partialFindings = partialFindings.ifEmpty { error.partialFindings },
                cause = error,
            )
        }
        val msg = error.message.orEmpty()
        val lower = msg.lowercase()
        if ("rate limit" in lower || "quota" in lower || "resource_exhausted" in lower || "429" in lower) {
            return DecisionPendingRetryException(
                message = "Gemini Decision 1 rate limit (50K TPM) reached. Verification is placed in pending status and will automatically re-verify in 60 seconds.",
                partialFindings = partialFindings,
                cause = error,
            )
        }
        if (error is java.net.SocketTimeoutException) {
            return DecisionPendingRetryException(
                message = "Gemini Decision 1 connection timed out. Verification is placed in pending status and will automatically re-verify in 60 seconds.",
                partialFindings = partialFindings,
                cause = error,
            )
        }
        if (msg.startsWith("Invalid Gemini API key") ||
            msg.startsWith("Gemini API key") ||
            msg.startsWith("Gemini Decision 1")
        ) {
            return IOException(msg, error)
        }
        return when (error) {
            is java.net.UnknownHostException,
            is java.net.ConnectException,
            -> IOException("No internet connection to reach Gemini Decision 1. Check your network and try again.", error)
            else -> IOException(msg.ifBlank { "Could not complete Gemini Decision 1 check. Please try again." }, error)
        }
    }

    private fun extractApiErrorMessage(body: String): String? {
        val root = VocabularyJsonFormat.parseTree(body) as? Map<*, *> ?: return null
        val err = root["error"] as? Map<*, *> ?: return null
        return err["message"] as? String
    }

    companion object {
        const val DECISIONS_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/decisions"
        private const val DECISION_TIMEOUT_MS = 30_000L
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 25_000
        private const val LOCAL_STATE_FILENAME = "vocabulary_decision_state_v1.json"
        private const val VERIFY_KEY_REQUEST_JSON =
            """{"model":"gemini-decision-1","input":"API key check.","questions":{"ping":{"type":"noul","instructions":"Is this a valid key check?"}}}"""
        const val MISSING_API_KEY_MESSAGE =
            "Gemini API key is missing. Open Settings -> Gemini API Key and paste your key to run AI checks."
    }
}

fun CreationExtras.geminiDecisionClient(): VocabularyDecisionClient {
    val app = checkNotNull(this[APPLICATION_KEY])
    return GeminiDecisionClient(
        settingsRepository = DataStoreSettingsRepository(app),
        context = app,
    )
}
