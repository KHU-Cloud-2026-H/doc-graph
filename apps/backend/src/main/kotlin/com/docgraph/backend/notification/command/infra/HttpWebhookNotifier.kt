package com.docgraph.backend.notification.command.infra

import com.docgraph.backend.notification.command.domain.WebhookNotifier
import com.docgraph.backend.config.ConditionalOnRealHttpAdapter
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.net.http.HttpClient
import java.time.Duration

@Component
@ConditionalOnRealHttpAdapter
class HttpWebhookNotifier(
    props: WebhookNotifierProperties,
) : WebhookNotifier {

    private val log = LoggerFactory.getLogger(javaClass)

    private val restClient: RestClient = run {
        val timeout = Duration.ofMillis(props.timeoutMs)
        val httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(timeout)
            .build()
        val factory = JdkClientHttpRequestFactory(httpClient).apply { setReadTimeout(timeout) }
        RestClient.builder().requestFactory(factory).build()
    }

    override fun send(url: String, message: String) {
        try {
            restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(WebhookPayload(text = message, content = message))
                .retrieve()
                .toBodilessEntity()
        } catch (e: RestClientException) {
            log.warn("webhook 발송 실패 url={}: {}", url, e.message)
        }
    }
}

data class WebhookPayload(
    val text: String,
    val content: String,
)
