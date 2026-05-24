package com.docgraph.backend.document.query.application

fun interface SearchDocumentNodesByProjectQuery {
    fun search(projectId: Long): List<DocumentNodeData>
}
