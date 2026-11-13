package com.ravibhaiya.quizzen.data

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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Thrown when `gemini-decision-1` hits a temporary rate limit (`HTTP 429` / `50K TPM`), server overload,
 * or network timeout so the UI can place verification in **Pending** status and automatically re-verify
 * after [retrySeconds] (default 60 seconds).
 */
class DecisionPendingRetryException(
    message: String,
    val retrySeconds: Int = DEFAULT_RETRY_SECONDS,
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
 */
class GeminiDecisionClient(
    private val settingsRepository: SettingsRepository? = null,
    private val apiKeyProvider: () -> String = { DataStoreSettingsRepository.effectiveGeminiApiKey() },
) : VocabularyDecisionClient {

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
    ): Result<List<VocabularyDecisionFinding>> {
        val storedKey = DataStoreSettingsRepository.sanitizeApiKey(
            settingsRepository?.geminiApiKey?.first().orEmpty(),
        )
        val apiKey = storedKey.ifEmpty {
            DataStoreSettingsRepository.sanitizeApiKey(apiKeyProvider())
        }

        if (apiKey.isEmpty()) {
            return Result.failure(IllegalStateException(MISSING_API_KEY_MESSAGE))
        }

        val plan = VocabularyDecisionRules.buildRequestPlan(entries, dismissedKeys)
            ?: return Result.success(emptyList())

        val cacheKey = "$apiKey:${plan.requestJson.hashCode()}"
        cachedResults[cacheKey]?.let { cached ->
            return Result.success(cached)
        }

        return withContext(Dispatchers.IO) {
            try {
                val responseBody = withTimeout(DECISION_TIMEOUT_MS) {
                    postDecisionRequest(apiKey, plan.requestJson)
                }
                val findings = VocabularyDecisionRules.parseResponse(responseBody, plan)
                cachedResults[cacheKey] = findings
                Result.success(findings)
            } catch (e: CancellationException) {
                if (e is kotlinx.coroutines.TimeoutCancellationException) {
                    Result.failure(
                        DecisionPendingRetryException(
                            message = "Gemini Decision 1 timed out. Verification is pending and will automatically retry in 60 seconds.",
                            cause = e,
                        ),
                    )
                } else {
                    throw e
                }
            } catch (e: DecisionPendingRetryException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(wrapDecisionError(e))
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

    private fun wrapDecisionError(error: Exception): IOException {
        if (error is DecisionPendingRetryException) return error
        val msg = error.message.orEmpty()
        val lower = msg.lowercase()
        if ("rate limit" in lower || "quota" in lower || "resource_exhausted" in lower || "429" in lower) {
            return DecisionPendingRetryException(
                message = "Gemini Decision 1 rate limit (50K TPM) reached. Verification is placed in pending status and will automatically re-verify in 60 seconds.",
                cause = error,
            )
        }
        if (error is java.net.SocketTimeoutException) {
            return DecisionPendingRetryException(
                message = "Gemini Decision 1 connection timed out. Verification is placed in pending status and will automatically re-verify in 60 seconds.",
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
        const val MISSING_API_KEY_MESSAGE =
            "Gemini API key is missing. Open Settings -> Gemini API Key and paste your key to run AI checks."
        private val cachedResults = java.util.concurrent.ConcurrentHashMap<String, List<VocabularyDecisionFinding>>()
    }
}

fun CreationExtras.geminiDecisionClient(): VocabularyDecisionClient =
    GeminiDecisionClient(DataStoreSettingsRepository(checkNotNull(this[APPLICATION_KEY])))
