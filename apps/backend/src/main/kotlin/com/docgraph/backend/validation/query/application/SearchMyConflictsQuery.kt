package com.docgraph.backend.validation.query.application

import com.docgraph.backend.web.PageResponse
import org.springframework.data.domain.Pageable

fun interface SearchMyConflictsQuery {
    fun search(filter: MyConflictStatusFilter, pageable: Pageable): PageResponse<MyConflictRow>
}
