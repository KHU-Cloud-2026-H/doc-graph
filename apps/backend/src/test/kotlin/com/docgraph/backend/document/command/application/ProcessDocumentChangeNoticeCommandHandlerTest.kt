package com.docgraph.backend.document.command.application

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
import org.springframework.context.ApplicationEventPublisher
import java.time.OffsetDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
