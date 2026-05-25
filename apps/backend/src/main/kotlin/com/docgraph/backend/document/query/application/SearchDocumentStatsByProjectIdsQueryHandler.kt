package com.docgraph.backend.document.query.application

import com.docgraph.backend.document.query.infra.DocumentQueryRepository
import org.springframework.stereotype.Service

@Service
class SearchDocumentStatsByProjectIdsQueryHandler(
    private val repository: DocumentQueryRepository,
) : SearchDocumentStatsByProjectIdsQuery {
    override fun search(projectIds: Collection<Long>): Map<Long, DocumentStats> =
        repository.searchStatsByProjectIds(projectIds)
}
