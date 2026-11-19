package com.ravibhaiya.quizzen.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class GeminiDecisionClientTest {

    @Test
    fun `verifyApiKey rejects blank input without calling network`() = runTest {
        var called = false
        val client = GeminiDecisionClient(
            requestExecutor = { _, _ ->
                called = true
                "{}"
            },
        )

        val result = client.verifyApiKey("   \n ")
        assertTrue(result.isFailure)
        assertTrue(!called)
    }

    @Test
    fun `verifyApiKey sanitizes candidate key and succeeds on valid response`() = runTest {
        var sentKey: String? = null
        var sentBody: String? = null
        val client = GeminiDecisionClient(
            requestExecutor = { apiKey, body ->
                sentKey = apiKey
                sentBody = body
                """{"answers":{"ping":{"value":true}}}"""
            },
        )

        val result = client.verifyApiKey("  \"AIzaSyTestKey123\" \n")
        assertTrue(result.isSuccess)
        assertEquals("AIzaSyTestKey123", sentKey)
        assertTrue(sentBody.orEmpty().contains("gemini-decision-1"))
    }

    @Test
    fun `verifyApiKey propagates failure message when key is rejected`() = runTest {
        val client = GeminiDecisionClient(
            requestExecutor = { _, _ ->
                throw IOException("Invalid Gemini API key. Open Settings -> Gemini API Key, paste a valid Google AI Studio key, and try again.")
            },
        )

        val result = client.verifyApiKey("AIzaBadKey")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("Invalid Gemini API key"))
    }
}
