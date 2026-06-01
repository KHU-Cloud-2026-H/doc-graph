package com.docgraph.backend.validation.query.application

import com.docgraph.backend.validation.query.infra.AiUsageQueryRepository
import com.docgraph.backend.web.PageResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class SearchAiUsageRecordsByProjectQueryHandler(
    private val repository: AiUsageQueryRepository,
) : SearchAiUsageRecordsByProjectQuery {

    override fun search(projectId: Long, pageable: Pageable): PageResponse<AiUsageRecordResponse> {
        val page = repository.findRecordsByProject(projectId, pageable)
        return PageResponse(
            content = page.content,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            page = page.number,
            size = page.size,
        )
    }
}
