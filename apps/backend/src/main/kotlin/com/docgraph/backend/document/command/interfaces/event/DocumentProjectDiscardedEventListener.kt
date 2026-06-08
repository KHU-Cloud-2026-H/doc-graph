package com.docgraph.backend.document.command.interfaces.event

import com.docgraph.backend.document.command.application.DiscardDocumentProjectCommand
import com.docgraph.backend.document.command.application.DiscardDocumentProjectCommandHandler
import com.docgraph.backend.project.command.domain.ProjectDiscardedEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class DocumentProjectDiscardedEventListener(
    private val handler: DiscardDocumentProjectCommandHandler,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: ProjectDiscardedEvent) {
        handler.handle(DiscardDocumentProjectCommand(event.projectId))
    }
}
