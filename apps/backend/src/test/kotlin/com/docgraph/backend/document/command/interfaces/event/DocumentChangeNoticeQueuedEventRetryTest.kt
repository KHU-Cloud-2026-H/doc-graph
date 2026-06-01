package com.docgraph.backend.document.command.interfaces.event

import com.docgraph.backend.document.command.domain.DocumentChangeKind
import com.docgraph.backend.document.command.domain.DocumentChangeNotice
import com.docgraph.backend.document.command.domain.DocumentChangeNoticeQueuedEvent
import com.docgraph.backend.document.command.domain.DocumentChangeNoticeRepository
import com.docgraph.backend.document.command.domain.NotionBlock
import com.docgraph.backend.document.command.domain.NotionDocumentClient
import com.docgraph.backend.document.command.domain.NotionPage
import com.docgraph.backend.document.command.domain.NotionPatchResult
import com.docgraph.backend.document.command.domain.NotionSearchPage
import com.docgraph.backend.event.OutboxStatus
import com.docgraph.backend.fixtures.SharedPostgresContainer
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpStatus
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import java.time.OffsetDateTime
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FakeRetryNotionDocumentClient : NotionDocumentClient {
    @Volatile var behavior: () -> NotionPage = { throw RuntimeException("unconfigured") }
    val invocations = AtomicInteger(0)

    override fun fetchPage(pageId: String, accessToken: String?): NotionPage {
        invocations.incrementAndGet()
        return behavior()
    }

    override fun fetchBlockChildren(blockId: String, accessToken: String?): List<NotionBlock> = emptyList()
    override fun searchPages(accessToken: String, query: String?, pageSize: Int): List<NotionSearchPage> = emptyList()
    override fun patchBlockText(
        notionBlockId: String,
        newText: String,
        expectedLastEditedAt: OffsetDateTime?,
        accessToken: String?,
    ): NotionPatchResult = NotionPatchResult.Success
}

@TestConfiguration
class RetryTestConfig {
    @Bean @Primary fun fakeRetryNotionClient() = FakeRetryNotionDocumentClient()
}

@Tag("component")
@SpringBootTest
@Import(RetryTestConfig::class, SharedPostgresContainer::class)
@TestPropertySource(
    properties = [
        "document.change-notice.fetch.max-attempts=3",
        "document.change-notice.fetch.retry-delay-ms=10",
        "document.change-notice.fetch.retry-multiplier=1.0",
        "document.change-notice.fetch.retry-max-delay-ms=20",
    ],
)
class DocumentChangeNoticeQueuedEventRetryTest @Autowired constructor(
    private val publisher: ApplicationEventPublisher,
    private val noticeRepository: DocumentChangeNoticeRepository,
    private val notionClient: FakeRetryNotionDocumentClient,
    private val em: EntityManager,
    private val txTemplate: TransactionTemplate,
) {

    @BeforeEach
    fun reset() {
        txTemplate.executeWithoutResult {
            em.createQuery("DELETE FROM DocumentChangeNotice").executeUpdate()
        }
        notionClient.invocations.set(0)
    }

    @Test
    fun `영구 실패(4xx) — 재시도 없이 1회 호출 후 FAILED dead-letter`() {
        val notice = savePendingNotice()
        notionClient.behavior = { throw HttpClientErrorException(HttpStatus.NOT_FOUND) }

        publisher.publishEvent(DocumentChangeNoticeQueuedEvent(notice.id))

        waitFor { reload(notice.id).status == OutboxStatus.FAILED }
        assertEquals(1, notionClient.invocations.get(), "비-429 4xx는 재시도하지 않는다")
        assertNotNull(reload(notice.id).failureReason)
    }

    @Test
    fun `transient 실패(5xx) — max-attempts 만큼 재시도 후 FAILED dead-letter`() {
        val notice = savePendingNotice()
        notionClient.behavior = { throw HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE) }

        publisher.publishEvent(DocumentChangeNoticeQueuedEvent(notice.id))

        waitFor { reload(notice.id).status == OutboxStatus.FAILED }
        assertEquals(3, notionClient.invocations.get(), "5xx는 max-attempts=3 만큼 재시도")
    }

    private fun savePendingNotice(): DocumentChangeNotice =
        noticeRepository.save(
            DocumentChangeNotice(
                changeKind = DocumentChangeKind.CONTENT_UPDATED,
                notionPageId = "page-retry",
                // workspaceId=null → accessToken null → fake fetchPage가 즉시 호출되어 throw
                notionWorkspaceId = null,
                occurredAt = OffsetDateTime.parse("2026-05-01T00:00:00Z"),
            ),
        )

    private fun reload(id: Long): DocumentChangeNotice = noticeRepository.findById(id).orElseThrow()

    private fun waitFor(timeoutMs: Long = 10_000, intervalMs: Long = 50, predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (predicate()) return
            Thread.sleep(intervalMs)
        }
        assertTrue(false, "timeout after ${timeoutMs}ms")
    }
}
