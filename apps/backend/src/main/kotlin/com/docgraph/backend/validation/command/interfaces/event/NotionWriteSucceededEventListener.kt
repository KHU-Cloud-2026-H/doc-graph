package com.docgraph.backend.validation.command.interfaces.event

import com.docgraph.backend.document.command.domain.NotionWriteSucceededEvent
import com.docgraph.backend.validation.command.application.ResolveConflictCommand
import com.docgraph.backend.validation.command.application.ResolveConflictCommandHandler
import com.docgraph.backend.validation.command.domain.ConflictFindingRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class NotionWriteSucceededEventListener(
    private val findingRepository: ConflictFindingRepository,
    private val handler: ResolveConflictCommandHandler,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: NotionWriteSucceededEvent) {
        val finding = findingRepository.findById(event.conflictFindingId) ?: return
        handler.handle(ResolveConflictCommand(conflictId = finding.conflictId, occurredAt = event.occurredAt))
    }
}
