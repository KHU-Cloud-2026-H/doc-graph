package com.docgraph.backend.validation.command.application

import com.docgraph.backend.document.query.application.FindDocumentByIdQuery
import com.docgraph.backend.graph.query.application.FindEdgeByIdQuery
import com.docgraph.backend.validation.command.domain.ConflictFindingNotFoundException
import com.docgraph.backend.validation.command.domain.ConflictFindingRepository
import com.docgraph.backend.validation.command.domain.ConflictRepository
import com.docgraph.backend.validation.command.domain.ProposalApprovedEvent
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
            ?: throw ConflictFindingNotFoundException(command.findingId)

        val conflict = conflictRepository.findById(finding.conflictId)
            ?: throw ConflictFindingNotFoundException(command.findingId)
        val edge = findEdgeById.find(conflict.edgeId)
            ?: error("edge not found for conflict ${conflict.id}: edgeId=${conflict.edgeId}")
        val target = findDocumentById.find(edge.targetDocumentId)
            ?: error("target document not found: ${edge.targetDocumentId}")

        // 낙관적 동시성: client가 조회 시점에 본 notionLastEditedAt과 현재 값을 비교.
        // OffsetDateTime.equals는 offset 표현(Z vs +09:00)·chronology까지 따져 같은 instant도
        // 다르게 보므로, instant 기준(isEqual)으로 비교한다.
        val actual = target.notionLastEditedAt
        val expected = command.expectedTargetNotionLastEditedAt
        val unchanged = when {
            actual == null || expected == null -> actual == expected
            else -> actual.isEqual(expected)
        }
        if (!unchanged) {
            throw StaleProposalException(command.findingId)
        }

        val now = OffsetDateTime.now()
        finding.approve(by = command.approvedBy, at = now)
        publisher.publishEvent(ProposalApprovedEvent(finding.id, command.approvedBy, now))
    }
}
