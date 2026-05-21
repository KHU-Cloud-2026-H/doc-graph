package com.docgraph.backend.validation.query.application

data class ConflictFindingDetail(
    val findingId: Long,
    val targetDocumentId: Long,
    val targetBlockId: String,
    val newText: String,
)
