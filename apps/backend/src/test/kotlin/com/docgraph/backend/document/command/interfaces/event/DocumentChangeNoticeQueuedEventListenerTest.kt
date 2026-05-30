package com.docgraph.backend.document.command.interfaces.event

import com.docgraph.backend.document.command.application.NotionFetchResult
import com.docgraph.backend.document.command.application.ProcessDocumentChangeNoticeCommandHandler
import com.docgraph.backend.document.command.domain.DocumentChangeKind
import com.docgraph.backend.document.command.domain.DocumentChangeNotice
import com.docgraph.backend.document.command.domain.DocumentChangeNoticeQueuedEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

@Tag("unit")
class DocumentChangeNoticeQueuedEventListenerTest {

    private val handler = mockk<ProcessDocumentChangeNoticeCommandHandler>(relaxed = true)
    private val listener = DocumentChangeNoticeQueuedEventListener(handler)

    @Test
    fun `recordAttempt null → fetchFromNotion·applyAndMarkSuccess 미호출`() {
        every { handler.recordAttempt(1L) } returns null

        listener.on(DocumentChangeNoticeQueuedEvent(noticeId = 1L))

        verify(exactly = 0) { handler.fetchFromNotion(any()) }
        verify(exactly = 0) { handler.applyAndMarkSuccess(any(), any()) }
    }

    @Test
    fun `fetchFromNotion null → applyAndMarkSuccess 미호출`() {
        val notice = pendingNotice(id = 2L)
        every { handler.recordAttempt(2L) } returns notice
        every { handler.fetchFromNotion(notice) } returns null

        listener.on(DocumentChangeNoticeQueuedEvent(noticeId = 2L))

        verify(exactly = 0) { handler.applyAndMarkSuccess(any(), any()) }
    }

    @Test
    fun `정상 흐름 — recordAttempt → fetchFromNotion → applyAndMarkSuccess 순 호출`() {
        val notice = pendingNotice(id = 3L)
        val result = mockk<NotionFetchResult>()
        every { handler.recordAttempt(3L) } returns notice
        every { handler.fetchFromNotion(notice) } returns result

        listener.on(DocumentChangeNoticeQueuedEvent(noticeId = 3L))

        verify(exactly = 1) { handler.applyAndMarkSuccess(notice, result) }
    }

    private fun pendingNotice(id: Long): DocumentChangeNotice =
        DocumentChangeNotice(
            changeKind = DocumentChangeKind.CONTENT_UPDATED,
            notionPageId = "page-1",
            occurredAt = OffsetDateTime.parse("2026-05-01T00:00:00Z"),
        ).also {
            val field = it.javaClass.getDeclaredField("id")
            field.isAccessible = true
            field.set(it, id)
        }
}
