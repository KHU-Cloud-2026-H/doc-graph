package com.docgraph.backend.validation.query.application

import com.docgraph.backend.web.PageResponse
import org.springframework.data.domain.Pageable

fun interface SearchConflictsByProjectQuery {
    fun search(projectId: Long, pageable: Pageable): PageResponse<ConflictResponse>
}