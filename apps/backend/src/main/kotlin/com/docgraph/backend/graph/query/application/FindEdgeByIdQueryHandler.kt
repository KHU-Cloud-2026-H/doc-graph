package com.docgraph.backend.graph.query.application

import com.docgraph.backend.graph.command.domain.DependencyEdge
import com.docgraph.backend.graph.command.domain.DependencyEdgeRepository
import org.springframework.stereotype.Service

@Service
class FindEdgeByIdQueryHandler(
    private val edgeRepository: DependencyEdgeRepository,
) : FindEdgeByIdQuery {
    override fun find(edgeId: Long): EdgeDetail? =
        edgeRepository.findById(edgeId)?.toDetail()
}

fun DependencyEdge.toDetail(): EdgeDetail =
    EdgeDetail(
        id = id,
        projectId = projectId,
        sourceDocumentId = sourceDocumentId,
        targetDocumentId = targetDocumentId,
        validationCriterion = validationCriterion,
    )