package com.docgraph.backend.document.query.application

import com.docgraph.backend.document.query.infra.DocumentQueryRepository
import org.springframework.stereotype.Service

@Service
class SearchDocumentIdsByAssigneeQueryHandler(
    private val repository: DocumentQueryRepository,
) : SearchDocumentIdsByAssigneeQuery {
    override fun search(memberId: Long): List<Long> =
        repository.searchIdsByAssignee(memberId)
}
