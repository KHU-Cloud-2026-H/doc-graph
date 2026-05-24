package com.docgraph.backend.document.command.domain

sealed class NotionPatchResult {
    object Success : NotionPatchResult()
}
