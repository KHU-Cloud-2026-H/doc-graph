package com.docgraph.backend.graph.command.interfaces.event

import com.docgraph.backend.graph.command.application.MarkEdgeConflictCommand
import com.docgraph.backend.graph.command.application.MarkEdgeConflictCommandHandler
import com.docgraph.backend.validation.command.domain.ConflictDetectedEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ConflictDetectedEventListener(
    private val handler: MarkEdgeConflictCommandHandler,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: ConflictDetectedEvent) {
        handler.handle(
            MarkEdgeConflictCommand(
                edgeId = event.edgeId,
                occurredAt = event.occurredAt,
            ),
        )
    }
}
