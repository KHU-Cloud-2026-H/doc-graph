package com.docgraph.backend.fixtures

import com.docgraph.backend.auth.command.domain.NotionOAuthClient
import com.docgraph.backend.auth.command.domain.NotionOAuthToken
import com.docgraph.backend.document.command.domain.NotionBlock
import com.docgraph.backend.document.command.domain.NotionDocumentClient
import com.docgraph.backend.document.command.domain.NotionPage
import com.docgraph.backend.document.command.domain.NotionPatchResult
import com.docgraph.backend.document.command.domain.NotionSearchPage
import com.docgraph.backend.validation.command.domain.ConflictDetectionInput
import com.docgraph.backend.validation.command.domain.ConflictDetectionResult
import com.docgraph.backend.validation.command.domain.ConflictDetector
import com.docgraph.backend.notification.command.domain.WebhookNotifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.OffsetDateTime

/**
 * adapter.http.real=false(테스트 JVM 기본)일 때 외부 HTTP 어댑터 대신 등록되는 stub.
 * 실 java.net.http.HttpClient를 만들지 않아 컨텍스트 캐시 누적 OOM을 차단한다.
 *
 * test 소스의 plain @Configuration이라 모든 @SpringBootTest 컨텍스트에 자동 포함된다.
 * 실 어댑터를 wire로 검증하는 contract 테스트는 adapter.http.real=true로 두어 이 stub을 비활성화한다.
 *
 * 동작 검증이 필요한 테스트는 각자 @MockkBean으로 대체한다. 여기 stub은
 * 충돌 없음·발송 no-op만 제공하고, 호출되면 안 되는 read 어댑터는 명시적으로 실패시킨다.
 */
@Configuration
class RealHttpAdapterStubConfig {

    @Bean
    @ConditionalOnProperty(name = ["adapter.http.real"], havingValue = "false")
    fun stubConflictDetector(): ConflictDetector =
        ConflictDetector { ConflictDetectionResult(conflicts = emptyList(), usage = null) }

    @Bean
    @ConditionalOnProperty(name = ["adapter.http.real"], havingValue = "false")
    fun stubWebhookNotifier(): WebhookNotifier = WebhookNotifier { _, _ -> }

    @Bean
    @ConditionalOnProperty(name = ["adapter.http.real"], havingValue = "false")
    fun stubNotionDocumentClient(): NotionDocumentClient = object : NotionDocumentClient {
        override fun fetchPage(pageId: String, accessToken: String?): NotionPage = unsupported()
        override fun fetchBlockChildren(blockId: String, accessToken: String?): List<NotionBlock> = unsupported()
        override fun searchPages(accessToken: String, query: String?, pageSize: Int): List<NotionSearchPage> = unsupported()
        override fun patchBlockText(
            notionBlockId: String,
            newText: String,
            expectedLastEditedAt: OffsetDateTime?,
            accessToken: String?,
        ): NotionPatchResult = unsupported()
    }

    @Bean
    @ConditionalOnProperty(name = ["adapter.http.real"], havingValue = "false")
    fun stubNotionOAuthClient(): NotionOAuthClient = object : NotionOAuthClient {
        override fun exchangeAuthorizationCode(code: String): NotionOAuthToken = unsupported()
    }

    private fun unsupported(): Nothing =
        throw UnsupportedOperationException(
            "real Notion 어댑터 호출 — adapter.http.real=false. 동작이 필요하면 @MockkBean으로 대체하거나 true로 opt-in",
        )
}
