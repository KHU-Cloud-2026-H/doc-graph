package com.docgraph.backend.document.command.domain

enum class DocumentChangeKind {
    CONTENT_UPDATED,
    PARENT_MOVED,
    INITIAL_SYNC,
}
