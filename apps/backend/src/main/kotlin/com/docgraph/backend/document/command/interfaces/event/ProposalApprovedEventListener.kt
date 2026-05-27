package com.docgraph.backend.document.command.interfaces.event

import com.docgraph.backend.document.command.domain.NotionDocumentClient
import com.docgraph.backend.document.command.domain.NotionPatchResult
import com.docgraph.backend.document.command.domain.NotionWriteSucceededEvent
import com.docgraph.backend.validation.command.domain.ProposalApprovedEvent
import com.docgraph.backend.validation.query.application.FindConflictFindingByIdQuery
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ProposalApprovedEventListener(
    private val findConflictFindingById: FindConflictFindingByIdQuery,
    private val notionDocumentClient: NotionDocumentClient,
    private val publisher: ApplicationEventPublisher,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: ProposalApprovedEvent) {
        val finding = findConflictFindingById.find(event.conflictFindingId) ?: return
        val result = notionDocumentClient.patchBlockText(
            notionBlockId = finding.targetBlockId,
            newText = finding.newText,
            expectedLastEditedAt = null,
        )
        if (result == NotionPatchResult.Success) {
            publisher.publishEvent(
                NotionWriteSucceededEvent(
                    conflictFindingId = finding.findingId,
                    occurredAt = event.occurredAt,
                ),
            )
        }
    }
}
