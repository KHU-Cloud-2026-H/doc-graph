package com.docgraph.backend.document.query.application

fun interface SearchDocumentIdsByAssigneeQuery {
    fun search(memberId: Long): List<Long>
}
