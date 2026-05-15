package com.docgraph.backend.document.query.application

import com.docgraph.backend.web.PageResponse
import org.springframework.data.domain.Pageable

fun interface SearchDocumentSummariesByProjectQuery {
    fun search(projectId: Long, pageable: Pageable): PageResponse<DocumentSummary>
}