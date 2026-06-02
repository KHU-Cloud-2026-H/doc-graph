package com.docgraph.backend.notification.command.infra

import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.notification.command.domain.WebhookNotifier
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource

@Tag("component")
@SpringBootTest
@Import(SharedPostgresContainer::class)
// 실 HttpWebhookNotifier를 wire(WireMock)로 검증 — 실 RestClient opt-in.
@TestPropertySource(properties = ["adapter.http.real=true"])
class HttpWebhookNotifierTest @Autowired constructor(
    private val notifier: WebhookNotifier,
) {

    @BeforeEach
    fun reset() {
        wireMock.resetAll()
    }

    @Test
    fun `happy — POST 수신, text·content 동일 메시지`() {
        wireMock.stubFor(post(urlEqualTo("/webhook")).willReturn(aResponse().withStatus(200)))

        notifier.send("http://localhost:${wireMock.port()}/webhook", "⚠️ 충돌 감지")

        wireMock.verify(
            postRequestedFor(urlEqualTo("/webhook"))
                .withRequestBody(matchingJsonPath("$.text", equalTo("⚠️ 충돌 감지")))
                .withRequestBody(matchingJsonPath("$.content", equalTo("⚠️ 충돌 감지")))
        )
    }

    @Test
    fun `발송 5xx — 예외 전파 안 함 (best-effort)`() {
        wireMock.stubFor(post(urlEqualTo("/webhook")).willReturn(aResponse().withStatus(503)))

        notifier.send("http://localhost:${wireMock.port()}/webhook", "msg")
    }

    @Test
    fun `연결 불가 URL — 예외 전파 안 함 (best-effort)`() {
        notifier.send("http://localhost:1/webhook", "msg")
    }

    companion object {
        private val wireMock = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort()).also { it.start() }

        @JvmStatic
        @AfterAll
        fun stopWireMock() {
            wireMock.stop()
        }
    }
}
