package com.docgraph.backend.fixtures

import com.docgraph.backend.auth.command.infra.notion.NotionOAuthRestClient
import com.docgraph.backend.document.command.infra.notion.NotionDocumentRestClient
import com.docgraph.backend.notification.command.infra.HttpWebhookNotifier
import com.docgraph.backend.validation.command.infra.openai.OpenAiConflictDetector
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Import
import kotlin.test.assertEquals

@Tag("component")
@SpringBootTest
@Import(SharedPostgresContainer::class)
class HttpAdapterGatingProbeTest @Autowired constructor(
    private val ctx: ApplicationContext,
) {
    @Test
    fun `adapter_http_real=false 기본 — 실 HTTP 어댑터 빈은 컨텍스트에 없어야 한다`() {
        assertEquals(0, ctx.getBeanNamesForType(OpenAiConflictDetector::class.java).size, "OpenAiConflictDetector present")
        assertEquals(0, ctx.getBeanNamesForType(HttpWebhookNotifier::class.java).size, "HttpWebhookNotifier present")
        assertEquals(0, ctx.getBeanNamesForType(NotionDocumentRestClient::class.java).size, "NotionDocumentRestClient present")
        assertEquals(0, ctx.getBeanNamesForType(NotionOAuthRestClient::class.java).size, "NotionOAuthRestClient present")
    }
}
