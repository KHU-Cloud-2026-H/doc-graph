package com.docgraph.backend.validation.command.interfaces.event

import com.docgraph.backend.graph.command.domain.DependencyEdgeRemovedEvent
import com.docgraph.backend.validation.command.application.ResolveConflictCommand
import com.docgraph.backend.validation.command.application.ResolveConflictCommandHandler
import com.docgraph.backend.validation.command.domain.ConflictRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class DependencyEdgeRemovedEventListener(
    private val conflictRepository: ConflictRepository,
    private val handler: ResolveConflictCommandHandler,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: DependencyEdgeRemovedEvent) {
        val conflict = conflictRepository.findFirstByEdgeIdAndResolvedAtIsNull(event.edgeId) ?: return
        handler.handle(ResolveConflictCommand(conflictId = conflict.id, occurredAt = event.occurredAt))
    }
}
