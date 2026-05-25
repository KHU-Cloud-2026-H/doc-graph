package com.docgraph.backend.graph.command.interfaces.event

import com.docgraph.backend.graph.command.application.ClearEdgeConflictCommand
import com.docgraph.backend.graph.command.application.ClearEdgeConflictCommandHandler
import com.docgraph.backend.validation.command.domain.ConflictResolvedEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ConflictResolvedEventListener(
    private val handler: ClearEdgeConflictCommandHandler,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: ConflictResolvedEvent) {
        handler.handle(
            ClearEdgeConflictCommand(
                edgeId = event.edgeId,
                occurredAt = event.occurredAt,
            ),
        )
    }
}
