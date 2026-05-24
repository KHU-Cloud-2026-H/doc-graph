package com.docgraph.backend.document.query.application

import com.docgraph.backend.document.query.infra.DocumentQueryRepository
import org.springframework.stereotype.Service

@Service
class SearchDocumentNodesByProjectQueryHandler(
    private val repository: DocumentQueryRepository,
) : SearchDocumentNodesByProjectQuery {
    override fun search(projectId: Long): List<DocumentNodeData> =
        repository.searchNodesByProject(projectId)
}
