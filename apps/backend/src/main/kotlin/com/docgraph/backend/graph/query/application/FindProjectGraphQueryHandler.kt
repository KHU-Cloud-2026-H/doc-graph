package com.docgraph.backend.graph.query.application

import com.docgraph.backend.document.query.application.DocumentNodeData
import com.docgraph.backend.document.query.application.SearchDocumentNodesByProjectQuery
import com.docgraph.backend.graph.command.domain.DependencyEdgeRepository
import com.docgraph.backend.graph.command.domain.EdgeProposalRepository
import org.springframework.stereotype.Service

@Service
class FindProjectGraphQueryHandler(
    private val edgeRepository: DependencyEdgeRepository,
    private val proposalRepository: EdgeProposalRepository,
    private val searchDocumentNodes: SearchDocumentNodesByProjectQuery,
) : FindProjectGraphQuery {
    override fun find(projectId: Long): ProjectGraphResponse =
        ProjectGraphResponse(
            nodes = searchDocumentNodes.search(projectId).map { it.toNode() },
            edges = edgeRepository.findAllByProjectId(projectId).map { it.toResponse() },
            proposals = proposalRepository.findAllByProjectId(projectId).map { it.toResponse() },
        )
}

fun DocumentNodeData.toNode(): GraphNodeResponse =
    GraphNodeResponse(
        id = id,
        title = title,
        type = type,
        assigneeMemberId = assigneeMemberId,
        icon = icon,
    )
