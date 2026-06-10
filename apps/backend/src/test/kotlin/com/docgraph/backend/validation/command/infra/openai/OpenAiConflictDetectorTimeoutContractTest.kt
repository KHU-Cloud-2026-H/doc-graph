package com.docgraph.backend.validation.command.infra.openai

import com.docgraph.backend.fixtures.OpenAiTestFixture
import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import com.docgraph.backend.validation.command.domain.FirstValidationInput
import org.springframework.web.client.ResourceAccessException

@Tag("component")
@SpringBootTest
@Import(SharedPostgresContainer::class)
@OpenAiTestFixture
@TestPropertySource(properties = ["ai.openai.read-timeout-ms=300"])
class OpenAiConflictDetectorTimeoutContractTest {

    @Autowired lateinit var detector: OpenAiConflictDetector

    @Test
    fun `응답 도착 전 read timeout 초과 시 ResourceAccessException 전파`() {
        wireMock.stubFor(
            post(urlEqualTo("/api/v1/chat/completions"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withFixedDelay(3_000)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"id":"x","object":"chat.completion","choices":[{"index":0,"message":{"role":"assistant","content":"{\"conflicts\":[]}"},"finish_reason":"stop"}]}""")
                )
        )

        assertThrows(ResourceAccessException::class.java) {
            detector.detect(FirstValidationInput(emptyList(), emptyList(), "c"))
        }
    }

    @Test
    fun `본문 수신 도중 read timeout이 끊어도 ResourceAccessException으로 환원`() {
        // 운영 장애 재현: 헤더는 도착했으나 chunked 본문을 흘리는 도중 read timeout이 발동하면
        // RestClient는 ResourceAccessException이 아닌 일반 RestClientException으로 환원한다.
        // 어댑터가 이를 전송 실패(재시도·TIMEOUT 분류 대상)로 되돌리는지 검증한다.
        wireMock.stubFor(
            post(urlEqualTo("/api/v1/chat/completions"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withChunkedDribbleDelay(20, 3_000)
                        .withBody("""{"id":"x","object":"chat.completion","choices":[{"index":0,"message":{"role":"assistant","content":"{\"conflicts\":[]}"},"finish_reason":"stop"}]}""")
                )
        )

        assertThrows(ResourceAccessException::class.java) {
            detector.detect(FirstValidationInput(emptyList(), emptyList(), "c"))
        }
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
