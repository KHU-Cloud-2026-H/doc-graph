package com.docgraph.backend.project.query.application

fun interface SearchAssignedDocumentTypesByUserIdQuery {
    fun search(userId: Long): List<AssignedDocumentType>
}
