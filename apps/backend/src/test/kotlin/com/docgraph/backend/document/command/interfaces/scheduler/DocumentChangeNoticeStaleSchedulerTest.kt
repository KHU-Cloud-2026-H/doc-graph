package com.docgraph.backend.document.command.interfaces.scheduler

import com.docgraph.backend.document.command.application.ProcessDocumentChangeNoticeCommandHandler
import com.docgraph.backend.document.command.domain.DocumentChangeKind
import com.docgraph.backend.document.command.domain.DocumentChangeNotice
import com.docgraph.backend.document.command.domain.DocumentChangeNoticeQueuedEvent
import com.docgraph.backend.document.command.domain.DocumentChangeNoticeRepository
import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.ninjasquad.springmockk.MockkBean
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.event.EventListener
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.time.OffsetDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DocumentChangeNoticeQueuedEventProbe {
    val latch = CountDownLatch(1)
    @Volatile var received: DocumentChangeNoticeQueuedEvent? = null

    @EventListener
    fun on(event: DocumentChangeNoticeQueuedEvent) {
        received = event
        latch.countDown()
    }
}

@TestConfiguration
class DocumentChangeNoticeStaleSchedulerTestConfig {
    @Bean
    fun documentChangeNoticeQueuedEventProbe() = DocumentChangeNoticeQueuedEventProbe()
}

@Tag("component")
@SpringBootTest
@Import(DocumentChangeNoticeStaleSchedulerTestConfig::class, SharedPostgresContainer::class)
class DocumentChangeNoticeStaleSchedulerTest {

    @Autowired lateinit var scheduler: DocumentChangeNoticeStaleScheduler
    @Autowired lateinit var repository: DocumentChangeNoticeRepository
    @Autowired lateinit var em: EntityManager
    @Autowired lateinit var txTemplate: TransactionTemplate
    @Autowired lateinit var probe: DocumentChangeNoticeQueuedEventProbe

    // 실제 Notion API 호출 봉인
    @MockkBean(relaxed = true)
    lateinit var processHandler: ProcessDocumentChangeNoticeCommandHandler

    @BeforeEach
    fun reset() {
        txTemplate.executeWithoutResult {
            em.createQuery("DELETE FROM DocumentChangeNotice").executeUpdate()
        }
    }

    @Test
    fun `stale PENDING notice → DocumentChangeNoticeQueuedEvent 재발화`() {
        val staleTime = OffsetDateTime.now().minus(Duration.ofMinutes(5))
        val notice = repository.save(
            DocumentChangeNotice(
                changeKind = DocumentChangeKind.CONTENT_UPDATED,
                notionPageId = "page-stale-1",
                occurredAt = OffsetDateTime.now(),
                lastAttemptAt = staleTime,
                attempts = 1,
            ),
        )

        scheduler.processStaleRows(Duration.ofMinutes(1))

        val fired = probe.latch.await(5, TimeUnit.SECONDS)
        assertTrue(fired, "stale PENDING notice가 DocumentChangeNoticeQueuedEvent를 발화하지 않음")
        assertEquals(notice.id, probe.received?.noticeId)
    }

    @Test
    fun `lastAttemptAt null인 신규 PENDING notice → 즉시 발화 대상`() {
        val notice = repository.save(
            DocumentChangeNotice(
                changeKind = DocumentChangeKind.CONTENT_UPDATED,
                notionPageId = "page-new-1",
                occurredAt = OffsetDateTime.now(),
            ),
        )

        scheduler.processStaleRows(Duration.ofSeconds(30))

        val fired = probe.latch.await(5, TimeUnit.SECONDS)
        assertTrue(fired, "lastAttemptAt=null인 notice가 stale 조회에서 누락됨")
        assertEquals(notice.id, probe.received?.noticeId)
    }
}
