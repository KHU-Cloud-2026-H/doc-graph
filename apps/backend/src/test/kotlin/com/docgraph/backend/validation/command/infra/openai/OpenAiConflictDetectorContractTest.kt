package com.docgraph.backend.validation.command.infra.openai

import com.docgraph.backend.document.query.application.Block
import com.docgraph.backend.validation.command.domain.ConflictDetectionResponseException
import com.docgraph.backend.validation.command.domain.FirstValidationInput
import com.docgraph.backend.fixtures.OpenAiTestFixture
import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Tag("component")
@SpringBootTest
@Import(SharedPostgresContainer::class)
@OpenAiTestFixture
class OpenAiConflictDetectorContractTest {

    @Autowired lateinit var detector: OpenAiConflictDetector

    @BeforeEach
    fun reset() {
        wireMock.resetAll()
    }

    @Test
    fun `happy — DetectedConflict 배열 반환 + 요청 본문에 model·strict response_format 포함`() {
        wireMock.stubFor(
            post(urlEqualTo("/api/v1/chat/completions"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(chatResponseBody("""{"conflicts":[{"source_block_ids":["s1"],"target_block_id":"t1","rationale":"reason","new_text":"fix","title":"제목"}]}"""))
                )
        )

        val result = detector.detect(
            FirstValidationInput(
                sourceBlocks = listOf(block("s1", "변경 본문")),
                targetBlocks = listOf(block("t1", "반대 본문")),
                criterion = "결정사항 반영 여부",
            ),
        )

        assertEquals(1, result.size)
        assertEquals(listOf("s1"), result[0].sourceBlockIds)
        assertEquals("t1", result[0].targetBlockId)
        assertEquals("reason", result[0].rationale)
        assertEquals("fix", result[0].newText)
        assertEquals("제목", result[0].title)

        wireMock.verify(
            postRequestedFor(urlEqualTo("/api/v1/chat/completions"))
                .withHeader("Authorization", equalTo("Bearer test"))
                .withRequestBody(matchingJsonPath("$.model", equalTo("test-model")))
                .withRequestBody(matchingJsonPath("$.response_format.type", equalTo("json_schema")))
                .withRequestBody(matchingJsonPath("$.response_format.json_schema.strict", equalTo("true")))
                .withRequestBody(matchingJsonPath("$.messages[0].role", equalTo("system")))
                .withRequestBody(matchingJsonPath("$.messages[1].role", equalTo("user")))
        )
    }

    @Test
    fun `empty conflicts — emptyList 반환`() {
        wireMock.stubFor(
            post(urlEqualTo("/api/v1/chat/completions"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(chatResponseBody("""{"conflicts":[]}"""))
                )
        )

        val result = detector.detect(FirstValidationInput(emptyList(), emptyList(), "c"))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `429 Too Many Requests — HttpClientErrorException 전파 (호출자 retry 흡수)`() {
        wireMock.stubFor(
            post(urlEqualTo("/api/v1/chat/completions"))
                .willReturn(aResponse().withStatus(429))
        )

        val ex = assertThrows(HttpClientErrorException::class.java) {
            detector.detect(FirstValidationInput(emptyList(), emptyList(), "c"))
        }
        assertEquals(429, ex.statusCode.value())
    }

    @Test
    fun `5xx — HttpServerErrorException 전파`() {
        wireMock.stubFor(
            post(urlEqualTo("/api/v1/chat/completions"))
                .willReturn(aResponse().withStatus(503))
        )

        val ex = assertThrows(HttpServerErrorException::class.java) {
            detector.detect(FirstValidationInput(emptyList(), emptyList(), "c"))
        }
        assertEquals(503, ex.statusCode.value())
    }

    @Test
    fun `401 Unauthorized — HttpClientErrorException 전파 (key 오설정)`() {
        wireMock.stubFor(
            post(urlEqualTo("/api/v1/chat/completions"))
                .willReturn(aResponse().withStatus(401))
        )

        val ex = assertThrows(HttpClientErrorException::class.java) {
            detector.detect(FirstValidationInput(emptyList(), emptyList(), "c"))
        }
        assertEquals(401, ex.statusCode.value())
    }

    @Test
    fun `스키마 불일치 content — ConflictDetectionResponseException 전파`() {
        wireMock.stubFor(
            post(urlEqualTo("/api/v1/chat/completions"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(chatResponseBody("""{"unexpected":"shape"}"""))
                )
        )

        assertThrows(ConflictDetectionResponseException::class.java) {
            detector.detect(FirstValidationInput(emptyList(), emptyList(), "c"))
        }
    }

    @Test
    fun `choices 없는 응답 — ConflictDetectionResponseException 전파`() {
        wireMock.stubFor(
            post(urlEqualTo("/api/v1/chat/completions"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"id":"x","object":"chat.completion","choices":[]}""")
                )
        )

        assertThrows(ConflictDetectionResponseException::class.java) {
            detector.detect(FirstValidationInput(emptyList(), emptyList(), "c"))
        }
    }

    private fun block(id: String, text: String) = Block(id, null, "paragraph", text, null, 0)

    private fun chatResponseBody(content: String): String {
        val escaped = content.replace("\\", "\\\\").replace("\"", "\\\"")
        return """
            {
              "id": "chatcmpl-test",
              "object": "chat.completion",
              "choices": [
                {"index": 0, "message": {"role": "assistant", "content": "$escaped"}, "finish_reason": "stop"}
              ]
            }
        """.trimIndent()
    }

    companion object {
        private val wireMock = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort()).also { it.start() }

        @JvmStatic
        @AfterAll
        fun stopWireMock() {
            wireMock.stop()
        }

        @JvmStatic
        @DynamicPropertySource
        fun overrideBaseUrl(registry: DynamicPropertyRegistry) {
            registry.add("ai.openai.base-url") { "http://localhost:${wireMock.port()}/api/v1" }
        }
    }
}
