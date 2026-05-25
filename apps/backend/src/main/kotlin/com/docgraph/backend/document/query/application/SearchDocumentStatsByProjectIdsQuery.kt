package com.docgraph.backend.document.query.application

fun interface SearchDocumentStatsByProjectIdsQuery {
    fun search(projectIds: Collection<Long>): Map<Long, DocumentStats>
}
