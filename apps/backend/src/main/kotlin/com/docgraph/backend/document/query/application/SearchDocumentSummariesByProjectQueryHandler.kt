package com.docgraph.backend.document.query.application

import com.docgraph.backend.document.query.infra.DocumentQueryRepository
import com.docgraph.backend.web.PageResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class SearchDocumentSummariesByProjectQueryHandler(
    private val repository: DocumentQueryRepository,
) : SearchDocumentSummariesByProjectQuery {
    override fun search(projectId: Long, pageable: Pageable): PageResponse<DocumentSummary> {
        val page = repository.searchSummariesByProject(projectId, pageable)
        return PageResponse(
            content = page.content,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            page = pageable.pageNumber,
            size = pageable.pageSize,
        )
    }
}