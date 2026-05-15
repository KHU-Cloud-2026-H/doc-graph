package com.docgraph.backend.validation.query.application

import com.docgraph.backend.event.OutboxStatus
import com.docgraph.backend.graph.query.application.SearchEdgeIdsByProjectQuery
import com.docgraph.backend.validation.query.infra.ValidationQueryRepository
import com.docgraph.backend.web.PageResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class SearchValidationTasksByProjectQueryHandler(
    private val searchEdgeIdsByProject: SearchEdgeIdsByProjectQuery,
    private val validationQueryRepository: ValidationQueryRepository,
) : SearchValidationTasksByProjectQuery {

    override fun search(projectId: Long, pageable: Pageable): PageResponse<ValidationTaskResponse> {
        val edgeIds = searchEdgeIdsByProject.search(projectId)
        if (edgeIds.isEmpty()) {
            return PageResponse(
                content = emptyList(),
                totalElements = 0L,
                totalPages = 0,
                page = pageable.pageNumber,
                size = pageable.pageSize,
            )
        }

        val taskPage = validationQueryRepository.findValidationTasksByEdgeIds(edgeIds, pageable)
        val responses = taskPage.content.map { task ->
            ValidationTaskResponse(
                id = task.id,
                edgeId = task.edgeId,
                status = when (task.status) {
                    OutboxStatus.PENDING -> ValidationStatus.PENDING
                    OutboxStatus.SUCCESS -> ValidationStatus.SUCCESS
                    OutboxStatus.FAILED -> ValidationStatus.FAILED
                },
                createdAt = task.createdAt,
            )
        }

        return PageResponse(
            content = responses,
            totalElements = taskPage.totalElements,
            totalPages = taskPage.totalPages,
            page = taskPage.number,
            size = taskPage.size,
        )
    }
}