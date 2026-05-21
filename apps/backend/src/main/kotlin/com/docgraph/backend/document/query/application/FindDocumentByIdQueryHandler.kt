package com.docgraph.backend.document.query.application

import com.docgraph.backend.document.query.infra.DocumentQueryRepository
import org.springframework.stereotype.Service

@Service
class FindDocumentByIdQueryHandler(
    private val repository: DocumentQueryRepository,
) : FindDocumentByIdQuery {
    override fun find(documentId: Long): DocumentDetail? = repository.findDetail(documentId)
}