package com.docgraph.backend.graph.query.application

import com.docgraph.backend.graph.command.domain.DependencyEdge
import com.docgraph.backend.graph.command.domain.DependencyEdgeRepository
import com.docgraph.backend.graph.command.domain.EdgeConflictStatus
import com.docgraph.backend.validation.query.application.ConflictStatus
import org.springframework.stereotype.Service

@Service
class SearchEdgesByProjectQueryHandler(
    private val edgeRepository: DependencyEdgeRepository,
) : SearchEdgesByProjectQuery {
    override fun search(projectId: Long): List<EdgeResponse> =
        edgeRepository.findAllByProjectId(projectId).map { it.toResponse() }
}

fun DependencyEdge.toResponse(): EdgeResponse =
    EdgeResponse(
        id = id,
        sourceDocumentId = sourceDocumentId,
        targetDocumentId = targetDocumentId,
        validationCriterion = validationCriterion,
        conflictStatus = conflictStatus.toResponse(),
        source = source,
        ruleId = ruleId,
    )

private fun EdgeConflictStatus.toResponse(): ConflictStatus =
    when (this) {
        EdgeConflictStatus.NONE -> ConflictStatus.NONE
        EdgeConflictStatus.CONFLICT -> ConflictStatus.CONFLICT
    }
