package com.docgraph.backend.document.query.application

import com.docgraph.backend.document.query.infra.DocumentQueryRepository
import org.springframework.stereotype.Service

@Service
class SearchDocumentReferencesByIdsQueryHandler(
    private val repository: DocumentQueryRepository,
) : SearchDocumentReferencesByIdsQuery {
    override fun search(documentIds: List<Long>): List<DocumentReference> =
        repository.findReferencesByIds(documentIds)
}
