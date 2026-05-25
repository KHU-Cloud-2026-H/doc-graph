package com.docgraph.backend.validation.query.application

import com.docgraph.backend.document.query.application.DocumentType

data class InboxDocumentRef(
    val id: Long,
    val title: String,
    val type: DocumentType?,
)
