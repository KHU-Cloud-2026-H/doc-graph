package com.docgraph.backend.document.command.application

data class ApplyApprovedBlockTextCommand(
    val targetDocumentId: Long,
    val targetBlockId: String,
    val newText: String,
)
