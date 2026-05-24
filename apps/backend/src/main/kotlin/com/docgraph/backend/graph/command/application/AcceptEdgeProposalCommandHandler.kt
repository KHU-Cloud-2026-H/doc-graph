package com.docgraph.backend.graph.command.application

import com.docgraph.backend.graph.command.domain.DependencyEdge
import com.docgraph.backend.graph.command.domain.DependencyEdgeRepository
import com.docgraph.backend.graph.command.domain.EdgeProposalNotFoundException
import com.docgraph.backend.graph.command.domain.EdgeProposalRepository
import com.docgraph.backend.graph.command.domain.ValidationPairCreatedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class AcceptEdgeProposalCommandHandler(
    private val proposalRepository: EdgeProposalRepository,
    private val edgeRepository: DependencyEdgeRepository,
    private val publisher: ApplicationEventPublisher,
) {
    @Transactional
    fun handle(command: AcceptEdgeProposalCommand): DependencyEdge {
        val proposal = proposalRepository.findByProjectIdAndId(command.projectId, command.proposalId)
            ?: throw EdgeProposalNotFoundException(command.proposalId)

        edgeRepository.findByProjectIdAndSourceDocumentIdAndTargetDocumentId(
            projectId = proposal.projectId,
            sourceDocumentId = proposal.sourceDocumentId,
            targetDocumentId = proposal.targetDocumentId,
        )?.let { existing ->
            proposalRepository.delete(proposal)
            return existing
        }

        val now = OffsetDateTime.now()
        val edge = edgeRepository.save(proposal.accept(now))
        proposalRepository.delete(proposal)

        publisher.publishEvent(
            ValidationPairCreatedEvent(
                validationPairId = command.validationPairId,
                edgeId = edge.id,
                occurredAt = now,
            ),
        )

        return edge
    }
}
