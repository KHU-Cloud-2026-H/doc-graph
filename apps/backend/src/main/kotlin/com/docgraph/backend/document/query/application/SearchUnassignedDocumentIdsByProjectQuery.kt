package com.docgraph.backend.document.query.application

fun interface SearchUnassignedDocumentIdsByProjectQuery {
    fun search(projectId: Long): List<Long>
}
