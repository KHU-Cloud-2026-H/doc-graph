package com.docgraph.backend.validation.command.application

import com.docgraph.backend.validation.command.domain.ConflictRepository
import com.docgraph.backend.validation.command.domain.ConflictResolvedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ResolveConflictCommandHandler(
    private val conflictRepository: ConflictRepository,
    private val publisher: ApplicationEventPublisher,
) {
    @Transactional
    fun handle(command: ResolveConflictCommand) {
        val conflict = conflictRepository.findById(command.conflictId) ?: return
        if (conflict.resolvedAt != null) return
        conflict.markResolved(command.occurredAt)
        conflictRepository.save(conflict)
        publisher.publishEvent(
            ConflictResolvedEvent(
                conflictId = conflict.id,
                edgeId = conflict.edgeId,
                occurredAt = command.occurredAt,
            ),
        )
    }
}
