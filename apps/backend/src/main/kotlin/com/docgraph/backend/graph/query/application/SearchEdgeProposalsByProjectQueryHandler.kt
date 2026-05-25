package com.docgraph.backend.graph.query.application

import com.docgraph.backend.graph.command.domain.EdgeProposal
import com.docgraph.backend.graph.command.domain.EdgeProposalRepository
import org.springframework.stereotype.Service

@Service
class SearchEdgeProposalsByProjectQueryHandler(
    private val proposalRepository: EdgeProposalRepository,
) : SearchEdgeProposalsByProjectQuery {
    override fun search(projectId: Long): List<EdgeProposalResponse> =
        proposalRepository.findAllByProjectId(projectId).map { it.toResponse() }
}

fun EdgeProposal.toResponse(): EdgeProposalResponse =
    EdgeProposalResponse(
        id = id,
        sourceDocumentId = sourceDocumentId,
        targetDocumentId = targetDocumentId,
        similarityScore = similarityScore,
        validationCriterion = validationCriterion,
    )
