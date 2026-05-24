package com.docgraph.backend.document.query.application

import com.docgraph.backend.project.query.application.AssignedDocumentType

fun interface SearchDocumentIdsByProjectAndTypesQuery {
    fun search(typeAssignments: List<AssignedDocumentType>): List<Long>
}
