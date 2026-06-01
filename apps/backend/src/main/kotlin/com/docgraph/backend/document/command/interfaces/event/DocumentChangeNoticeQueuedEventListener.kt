package com.docgraph.backend.document.command.interfaces.event

import com.docgraph.backend.document.command.application.ProcessDocumentChangeNoticeCommandHandler
import com.docgraph.backend.document.command.domain.DocumentChangeNoticeQueuedEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class DocumentChangeNoticeQueuedEventListener(
    private val handler: ProcessDocumentChangeNoticeCommandHandler,
) {
    @Async
    @EventListener
    fun on(event: DocumentChangeNoticeQueuedEvent) {
        val notice = handler.recordAttempt(event.noticeId) ?: return
        val result = handler.fetchFromNotion(notice) ?: return
        handler.applyAndMarkSuccess(notice, result)
    }
}
