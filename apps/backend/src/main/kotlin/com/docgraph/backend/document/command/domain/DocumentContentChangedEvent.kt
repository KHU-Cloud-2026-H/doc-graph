package com.docgraph.backend.document.command.domain

data class DocumentContentChangedEvent(
    val documentId: Long,
    val projectId: Long,
    val notionPageId: String,
)
