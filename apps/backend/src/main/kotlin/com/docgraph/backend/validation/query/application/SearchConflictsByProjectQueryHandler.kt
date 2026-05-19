package com.docgraph.backend.validation.query.application

import com.docgraph.backend.graph.query.application.EdgeDetail
import com.docgraph.backend.graph.query.application.SearchEdgeDetailsByProjectQuery
import com.docgraph.backend.validation.query.infra.ValidationQueryRepository
import com.docgraph.backend.web.PageResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class SearchConflictsByProjectQueryHandler(
    private val searchEdgeDetailsByProject: SearchEdgeDetailsByProjectQuery,
    private val validationQueryRepository: ValidationQueryRepository,
) : SearchConflictsByProjectQuery {

    override fun search(projectId: Long, pageable: Pageable): PageResponse<ConflictResponse> {
        val edgeDetails: List<EdgeDetail> = searchEdgeDetailsByProject.search(projectId)
        if (edgeDetails.isEmpty()) {
            return PageResponse(
                content = emptyList(),
                totalElements = 0L,
                totalPages = 0,
                page = pageable.pageNumber,
                size = pageable.pageSize,
            )
        }

        val edgeIds = edgeDetails.map { it.id }
        val edgeDetailById = edgeDetails.associateBy { it.id }

        val conflictPage = validationQueryRepository.findConflictsByEdgeIds(edgeIds, pageable)
        val conflictIds = conflictPage.content.map { it.id }
        val findingsByConflictId = validationQueryRepository.findFindingsByConflictIds(conflictIds)
            .groupBy { it.conflictId }

        val responses = conflictPage.content.map { conflict ->
            val edge = edgeDetailById.getValue(conflict.edgeId)
            val findings = findingsByConflictId[conflict.id].orEmpty().map { row ->
                ConflictFindingResponse(
                    id = row.id,
                    sourceBlockIds = row.sourceBlockIds,
                    targetBlockId = row.targetBlockId,
                    rationale = row.rationale,
                    newText = row.newText,
                    detectedAt = row.detectedAt,
                )
            }
            ConflictResponse(
                id = conflict.id,
                edgeId = conflict.edgeId,
                sourceDocumentId = edge.sourceDocumentId,
                targetDocumentId = edge.targetDocumentId,
                findings = findings,
                ignoredAt = conflict.ignoredAt,
                ignoreReason = conflict.ignoreReason,
            )
        }

        return PageResponse(
            content = responses,
            totalElements = conflictPage.totalElements,
            totalPages = conflictPage.totalPages,
            page = conflictPage.number,
            size = conflictPage.size,
        )
    }
}
