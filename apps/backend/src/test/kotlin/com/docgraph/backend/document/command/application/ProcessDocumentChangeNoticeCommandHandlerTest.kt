package com.docgraph.backend.document.command.application

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.docgraph.backend.auth.command.domain.NotionConnectionRepository
import com.docgraph.backend.auth.command.infra.NotionAccessTokenDecryptor
import com.docgraph.backend.document.command.domain.Block
import com.docgraph.backend.document.command.domain.BlockRepository
import com.docgraph.backend.document.command.domain.Document
import com.docgraph.backend.document.command.domain.DocumentChangeKind
import com.docgraph.backend.document.command.domain.DocumentChangeNotice
import com.docgraph.backend.document.command.domain.DocumentChangeNoticeRepository
import com.docgraph.backend.document.command.domain.DocumentContentChangedEvent
import com.docgraph.backend.document.command.domain.DocumentRepository
import com.docgraph.backend.document.command.domain.NotionDocumentClient
import com.docgraph.backend.event.OutboxStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import java.time.OffsetDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Tag("unit")
class ProcessDocumentChangeNoticeCommandHandlerTest {

    private val noticeRepository = mockk<DocumentChangeNoticeRepository>()
    private val documentRepository = mockk<DocumentRepository>()
    private val blockRepository = mockk<BlockRepository>(relaxed = true)
    private val notionDocumentClient = mockk<NotionDocumentClient>()
    private val notionConnectionRepository = mockk<NotionConnectionRepository>()
    private val notionAccessTokenDecryptor = mockk<NotionAccessTokenDecryptor>()
    private val publisher = mockk<ApplicationEventPublisher>(relaxed = true)

    private val handler = ProcessDocumentChangeNoticeCommandHandler(
        noticeRepository = noticeRepository,
        documentRepository = documentRepository,
        blockRepository = blockRepository,
        notionDocumentClient = notionDocumentClient,
        notionConnectionRepository = notionConnectionRepository,
        notionAccessTokenDecryptor = notionAccessTokenDecryptor,
        publisher = publisher,
    )

    @Test
    fun `recordAttempt — PENDING notice → attempts 증가 후 반환`() {
        val notice = pendingNotice()
        every { noticeRepository.findById(1L) } returns Optional.of(notice)
        every { noticeRepository.save(notice) } returns notice

        val result = handler.recordAttempt(1L)

        assertEquals(notice, result)
        assertEquals(1, notice.attempts)
    }

    @Test
    fun `recordAttempt — SUCCESS notice → null 반환 (재처리 방지)`() {
        val notice = pendingNotice().apply { markSuccess() }
        every { noticeRepository.findById(1L) } returns Optional.of(notice)

        assertNull(handler.recordAttempt(1L))
        verify(exactly = 0) { noticeRepository.save(any()) }
    }

    @Test
    fun `recordAttempt — notice 미존재 → null 반환`() {
        every { noticeRepository.findById(99L) } returns Optional.empty()

        assertNull(handler.recordAttempt(99L))
    }

    @Test
    fun `fetchFromNotion — Notion 호출 실패 시 예외 전파 (swallow 제거)`() {
        val notice = pendingNotice()
        every { notionConnectionRepository.findAllByNotionWorkspaceId("ws-1") } returns emptyList()
        every { notionDocumentClient.fetchPage("page-1", null) } throws RuntimeException("notion down")

        assertFailsWith<RuntimeException> { handler.fetchFromNotion(notice) }
    }

    @Test
    fun `markFailed — PENDING notice → FAILED 전이 + reason 저장`() {
        val notice = pendingNotice()
        every { noticeRepository.findById(1L) } returns Optional.of(notice)
        every { noticeRepository.save(notice) } returns notice

        handler.markFailed(1L, "notion 404")

        assertEquals(OutboxStatus.FAILED, notice.status)
        assertEquals("notion 404", notice.failureReason)
    }

    @Test
    fun `markFailed — 비-PENDING notice → 전이 없음 (성공 덮어쓰기 방지)`() {
        val notice = pendingNotice().apply { markSuccess() }
        every { noticeRepository.findById(1L) } returns Optional.of(notice)

        handler.markFailed(1L, "late failure")

        assertEquals(OutboxStatus.SUCCESS, notice.status)
        verify(exactly = 0) { noticeRepository.save(any()) }
    }

    @Test
    fun `applyAndMarkSuccess — document 없음 → markFailed 저장`() {
        val notice = pendingNotice()
        val result = notionFetchResult()
        every { documentRepository.findByNotionPageId("page-1") } returns emptyList()
        every { noticeRepository.save(notice) } returns notice

        handler.applyAndMarkSuccess(notice, result)

        assertEquals(OutboxStatus.FAILED, notice.status)
        verify(exactly = 0) { publisher.publishEvent(any()) }
    }

    @Test
    fun `applyAndMarkSuccess — 정상 → document 갱신 + DocumentContentChangedEvent 발행 + SUCCESS`() {
        val notice = pendingNotice()
        val result = notionFetchResult()
        val document = document()

        every { documentRepository.findByNotionPageId("page-1") } returns listOf(document)
        every { documentRepository.save(document) } returns document
        every { blockRepository.findByDocument_IdOrderBySortOrderAsc(10L) } returns emptyList()
        every { noticeRepository.save(notice) } returns notice

        val eventSlot = slot<DocumentContentChangedEvent>()
        every { publisher.publishEvent(capture(eventSlot)) } returns Unit

        handler.applyAndMarkSuccess(notice, result)

        assertEquals(OutboxStatus.SUCCESS, notice.status)
        assertEquals(10L, eventSlot.captured.documentId)
        assertEquals(42L, eventSlot.captured.projectId)
        assertEquals("page-1", eventSlot.captured.notionPageId)
        assertEquals("갱신된 타이틀", document.title)
        assertEquals("본문 텍스트", document.flatText)
    }

    @Test
    fun `applyAndMarkSuccess — 같은 notionPageId가 여러 document에 매칭되면 WARN 로그 (고아·중복 탐지)`() {
        val notice = pendingNotice()
        val result = notionFetchResult()
        val chosen = document()
        val duplicate = Document(projectId = 99L, notionPageId = "page-1", title = "중복").also { setId(it, 11L) }

        every { documentRepository.findByNotionPageId("page-1") } returns listOf(chosen, duplicate)
        every { documentRepository.save(chosen) } returns chosen
        every { blockRepository.findByDocument_IdOrderBySortOrderAsc(10L) } returns emptyList()
        every { noticeRepository.save(notice) } returns notice
        every { publisher.publishEvent(any()) } returns Unit

        val logger = LoggerFactory.getLogger(ProcessDocumentChangeNoticeCommandHandler::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)
        try {
            handler.applyAndMarkSuccess(notice, result)
        } finally {
            logger.detachAppender(appender)
        }

        val warns = appender.list.filter { it.level == Level.WARN }
        assertTrue(
            warns.any { it.formattedMessage.contains("page-1") && it.formattedMessage.contains("2") },
            "다중 매칭 시 page id와 개수를 담은 WARN 로그를 남겨야 한다. actual=${warns.map { it.formattedMessage }}",
        )
        // 다중 매칭이어도 처리 자체는 정상 진행 (firstOrNull = chosen)
        assertEquals(OutboxStatus.SUCCESS, notice.status)
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private fun pendingNotice() = DocumentChangeNotice(
        changeKind = DocumentChangeKind.CONTENT_UPDATED,
        notionEventId = "evt-1",
        eventType = "page.content_updated",
        notionWorkspaceId = "ws-1",
        notionPageId = "page-1",
        occurredAt = OffsetDateTime.parse("2026-05-01T00:00:00Z"),
    ).also { setId(it, 1L) }

    private fun notionFetchResult() = NotionFetchResult(
        title = "갱신된 타이틀",
        rawJson = "{}",
        createdBy = null,
        lastEditedBy = null,
        lastEditedTime = null,
        icon = null,
        blocks = listOf(
            com.docgraph.backend.document.command.domain.NotionBlock(
                id = "block-1",
                type = "paragraph",
                parentType = "page",
                parentId = "page-1",
                text = "본문 텍스트",
                linkedPageIds = emptySet(),
                childPageTitle = null,
                createdTime = null,
                lastEditedTime = null,
                createdBy = null,
                lastEditedBy = null,
                hasChildren = false,
                archived = false,
                inTrash = false,
                rawJson = "{}",
            ),
        ),
    )

    private fun document() = Document(
        projectId = 42L,
        notionPageId = "page-1",
        title = "원본 타이틀",
    ).also { setId(it, 10L) }

    private fun setId(entity: Any, id: Long) {
        val field = entity.javaClass.getDeclaredField("id")
        field.isAccessible = true
        field.set(entity, id)
    }
}
