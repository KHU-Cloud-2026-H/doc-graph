package com.docgraph.backend.validation.query.application

data class ConflictFindingDetailRow(
    val findingId: Long,
    val edgeId: Long,
    val targetBlockId: String,
    val newText: String,
)
