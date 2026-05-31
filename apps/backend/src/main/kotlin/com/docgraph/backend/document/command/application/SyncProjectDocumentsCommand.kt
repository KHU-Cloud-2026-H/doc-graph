package com.docgraph.backend.document.command.application

data class SyncProjectDocumentsCommand(
    val projectId: Long,
    val requestedBy: Long,
)
