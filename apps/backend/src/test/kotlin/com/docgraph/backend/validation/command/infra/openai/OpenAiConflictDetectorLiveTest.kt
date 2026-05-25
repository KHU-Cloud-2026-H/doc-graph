package com.docgraph.backend.validation.command.infra.openai

import com.docgraph.backend.document.query.application.Block
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.web.client.RestClientException
import tools.jackson.module.kotlin.jacksonObjectMapper
import kotlin.test.assertNotNull

@Tag("manual")
@EnabledIfEnvironmentVariable(named = "LIVE_OPENAI_TEST", matches = "true")
class OpenAiConflictDetectorLiveTest {

    @Test
    fun `real OpenAI API smoke test`() {
        val apiKey = requireEnv("AI_OPENAI_API_KEY")
        val model = requireEnv("AI_OPENAI_MODEL")
        val baseUrl = System.getenv("AI_OPENAI_BASE_URL") ?: "https://factchat-cloud.mindlogic.ai"
        val chatCompletionsPath = System.getenv("AI_OPENAI_CHAT_COMPLETIONS_PATH") ?: "/v1/gateway/chat/completions"
        val responseFormatEnabled = System.getenv("AI_OPENAI_RESPONSE_FORMAT_ENABLED")?.toBooleanStrictOrNull() ?: true

        val detector = OpenAiConflictDetector(
            restClient = OpenAiAdapterConfig().openAiRestClient(
                OpenAiProperties(
                    apiKey = apiKey,
                    model = model,
                    baseUrl = baseUrl,
                    timeoutMs = 30000,
                    chatCompletionsPath = chatCompletionsPath,
                    responseFormatEnabled = responseFormatEnabled,
                ),
            ),
            parser = ConflictDetectionResponseParser(jacksonObjectMapper()),
            props = OpenAiProperties(
                apiKey = apiKey,
                model = model,
                baseUrl = baseUrl,
                timeoutMs = 30000,
                chatCompletionsPath = chatCompletionsPath,
                responseFormatEnabled = responseFormatEnabled,
            ),
        )

        val result = try {
            detector.detect(
                changedBlocks = listOf(block("source-1", "The API accepts status values OPEN and CLOSED.")),
                counterpartBlocks = listOf(block("target-1", "The database enum only allows TODO and DONE.")),
                criterion = "Detect implementation-level consistency conflicts.",
            )
        } catch (ex: RestClientException) {
            throw AssertionError("OpenAI API request failed: ${ex.message}", ex)
        }

        assertNotNull(result)
    }

    private fun requireEnv(name: String): String =
        System.getenv(name)?.takeIf { it.isNotBlank() }
            ?: error("$name is required for LIVE_OPENAI_TEST=true")

    private fun block(id: String, text: String) = Block(id, null, "paragraph", text, 0)
}
