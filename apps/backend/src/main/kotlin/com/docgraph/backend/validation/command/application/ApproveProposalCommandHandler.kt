package com.docgraph.backend.validation.command.application

import com.docgraph.backend.document.query.application.FindDocumentByIdQuery
import com.docgraph.backend.graph.query.application.FindEdgeByIdQuery
import com.docgraph.backend.validation.command.domain.ConflictFindingNotFoundException
import com.docgraph.backend.validation.command.domain.ConflictFindingRepository
import com.docgraph.backend.validation.command.domain.ConflictRepository
import com.docgraph.backend.validation.command.domain.ProposalApproved
import com.docgraph.backend.validation.command.domain.StaleProposalException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class ApproveProposalCommandHandler(
    private val findingRepository: ConflictFindingRepository,
    private val conflictRepository: ConflictRepository,
    private val findEdgeById: FindEdgeByIdQuery,
    private val findDocumentById: FindDocumentByIdQuery,
    private val publisher: ApplicationEventPublisher,
) {
    @Transactional
    fun handle(command: ApproveProposalCommand) {
        val finding = findingRepository.findById(command.findingId)
            .orElseThrow { ConflictFindingNotFoundException(command.findingId) }

        val conflict = conflictRepository.findById(finding.conflictId)
            .orElseThrow { ConflictFindingNotFoundException(command.findingId) }
        val edge = findEdgeById.find(conflict.edgeId)
            ?: error("edge not found for conflict ${conflict.id}: edgeId=${conflict.edgeId}")
        val target = findDocumentById.find(edge.targetDocumentId)
            ?: error("target document not found: ${edge.targetDocumentId}")

        if (target.notionLastEditedAt != command.expectedTargetNotionLastEditedAt) {
            throw StaleProposalException(command.findingId)
        }

        val now = OffsetDateTime.now()
        finding.approve(by = command.approvedBy, at = now)
        publisher.publishEvent(ProposalApproved(finding.id, command.approvedBy, now))
    }
}
