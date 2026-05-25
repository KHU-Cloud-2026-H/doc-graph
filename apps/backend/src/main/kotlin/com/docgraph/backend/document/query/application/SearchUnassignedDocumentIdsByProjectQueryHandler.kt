package com.docgraph.backend.document.query.application

import com.docgraph.backend.document.query.infra.DocumentQueryRepository
import org.springframework.stereotype.Service

@Service
class SearchUnassignedDocumentIdsByProjectQueryHandler(
    private val repository: DocumentQueryRepository,
) : SearchUnassignedDocumentIdsByProjectQuery {
    override fun search(projectId: Long): List<Long> =
        repository.searchUnassignedIdsByProject(projectId)
}
