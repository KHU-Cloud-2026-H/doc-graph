package com.docgraph.backend.graph.command.application

import com.docgraph.backend.graph.command.domain.DependencyEdgeRepository
import com.docgraph.backend.graph.command.domain.EdgeProposal
import com.docgraph.backend.graph.command.domain.EdgeProposalRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 키워드 매칭으로 추려진 문서 쌍을 연결 제안(EdgeProposal)으로 저장한다.
 *
 * 제안은 아직 검증 대상이 아니므로 ValidationPairCreatedEvent를 발행하지 않는다 — 검증은
 * Admin이 제안을 수락(AcceptEdgeProposalCommand)해 DependencyEdge로 전환될 때 트리거된다.
 */
@Service
class ProposeEdgeCommandHandler(
    private val proposalRepository: EdgeProposalRepository,
    private val edgeRepository: DependencyEdgeRepository,
) {
    @Transactional
    fun handle(command: ProposeEdgeCommand): EdgeProposal? {
        edgeRepository.findByProjectIdAndSourceDocumentIdAndTargetDocumentId(
            projectId = command.projectId,
            sourceDocumentId = command.sourceDocumentId,
            targetDocumentId = command.targetDocumentId,
        )?.let { return null }

        proposalRepository.findByProjectIdAndSourceDocumentIdAndTargetDocumentId(
            projectId = command.projectId,
            sourceDocumentId = command.sourceDocumentId,
            targetDocumentId = command.targetDocumentId,
        )?.let { return it }

        return proposalRepository.save(
            EdgeProposal(
                projectId = command.projectId,
                sourceDocumentId = command.sourceDocumentId,
                targetDocumentId = command.targetDocumentId,
                ruleId = command.ruleId,
                validationCriterion = command.validationCriterion,
                similarityScore = command.similarityScore,
            ),
        )
    }
}
