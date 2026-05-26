package com.docgraph.backend.document.command.interfaces.event

import com.docgraph.backend.document.command.application.SyncProjectDocumentsCommand
import com.docgraph.backend.document.command.application.SyncProjectDocumentsCommandHandler
import com.docgraph.backend.project.command.domain.ProjectSyncTriggeredEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ProjectSyncTriggeredEventListener(
    private val handler: SyncProjectDocumentsCommandHandler,
) {
    @EventListener
    fun on(event: ProjectSyncTriggeredEvent) {
        handler.handle(
            SyncProjectDocumentsCommand(
                projectId = event.projectId,
                requestedBy = event.triggeredBy,
            ),
        )
    }
}
