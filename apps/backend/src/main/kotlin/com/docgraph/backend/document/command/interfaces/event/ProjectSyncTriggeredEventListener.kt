package com.docgraph.backend.document.command.interfaces.event

import com.docgraph.backend.document.command.application.SyncProjectDocumentsCommand
import com.docgraph.backend.document.command.application.SyncProjectDocumentsCommandHandler
import com.docgraph.backend.project.command.domain.ProjectSyncTriggeredEvent
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ProjectSyncTriggeredEventListener(
    private val handler: SyncProjectDocumentsCommandHandler,
) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: ProjectSyncTriggeredEvent) {
        handler.handle(
            SyncProjectDocumentsCommand(
                projectId = event.projectId,
                requestedBy = event.triggeredBy,
            ),
        )
    }
}
