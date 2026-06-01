package com.docgraph.backend.validation.command.interfaces.event

import com.docgraph.backend.document.command.domain.NotionWriteSucceededEvent
import com.docgraph.backend.validation.command.application.ResolveConflictCommand
import com.docgraph.backend.validation.command.application.ResolveConflictCommandHandler
import com.docgraph.backend.validation.command.domain.ConflictFindingRepository
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class NotionWriteSucceededEventListener(
    private val findingRepository: ConflictFindingRepository,
    private val handler: ResolveConflictCommandHandler,
) {
    // NotionWriteSucceededEvent는 ProposalApprovedEventListener(@Async @TransactionalEventListener,
    // 트랜잭션 밖)에서 발행된다. @TransactionalEventListener(AFTER_COMMIT)는 활성 트랜잭션이 없으면
    // 스킵되므로 plain @EventListener로 발행 즉시 처리한다(ResolveConflictCommandHandler가 자체 tx).
    @EventListener
    fun on(event: NotionWriteSucceededEvent) {
        val finding = findingRepository.findById(event.conflictFindingId) ?: return
        handler.handle(ResolveConflictCommand(conflictId = finding.conflictId, occurredAt = event.occurredAt))
    }
}
