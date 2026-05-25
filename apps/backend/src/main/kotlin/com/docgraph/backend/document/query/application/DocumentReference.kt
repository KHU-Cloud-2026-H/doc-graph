package com.docgraph.backend.document.query.application

data class DocumentReference(
    val id: Long,
    val projectId: Long,
    val title: String,
    val type: DocumentType?,
)
