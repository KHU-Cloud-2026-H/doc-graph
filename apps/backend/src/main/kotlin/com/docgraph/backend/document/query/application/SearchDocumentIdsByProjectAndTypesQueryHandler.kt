package com.docgraph.backend.document.query.application

import com.docgraph.backend.document.query.infra.DocumentQueryRepository
import com.docgraph.backend.project.query.application.AssignedDocumentType
import org.springframework.stereotype.Service

@Service
class SearchDocumentIdsByProjectAndTypesQueryHandler(
    private val repository: DocumentQueryRepository,
) : SearchDocumentIdsByProjectAndTypesQuery {
    override fun search(typeAssignments: List<AssignedDocumentType>): List<Long> =
        repository.searchIdsByProjectAndTypes(typeAssignments)
}
