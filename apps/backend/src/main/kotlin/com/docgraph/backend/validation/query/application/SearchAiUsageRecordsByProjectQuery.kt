package com.docgraph.backend.validation.query.application

import com.docgraph.backend.web.PageResponse
import org.springframework.data.domain.Pageable

fun interface SearchAiUsageRecordsByProjectQuery {
    fun search(projectId: Long, pageable: Pageable): PageResponse<AiUsageRecordResponse>
}
