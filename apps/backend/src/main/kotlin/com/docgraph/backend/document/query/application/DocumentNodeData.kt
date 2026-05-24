package com.docgraph.backend.document.query.application

data class DocumentNodeData(
    val id: Long,
    val title: String,
    val type: DocumentType?,
    val assigneeMemberId: Long?,
)
