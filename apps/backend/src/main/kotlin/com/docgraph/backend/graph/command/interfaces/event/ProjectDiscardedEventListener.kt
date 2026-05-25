package com.docgraph.backend.graph.command.interfaces.event

import com.docgraph.backend.graph.command.application.DiscardGraphProjectCommand
import com.docgraph.backend.graph.command.application.DiscardGraphProjectCommandHandler
import com.docgraph.backend.project.command.domain.ProjectDiscardedEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ProjectDiscardedEventListener(
    private val handler: DiscardGraphProjectCommandHandler,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: ProjectDiscardedEvent) {
        handler.handle(DiscardGraphProjectCommand(event.projectId))
    }
}
