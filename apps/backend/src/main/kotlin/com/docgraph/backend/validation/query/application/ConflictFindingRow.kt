package com.docgraph.backend.validation.query.application

import java.time.OffsetDateTime

data class ConflictFindingRow(
    val id: Long,
    val conflictId: Long,
    val sourceBlockIds: List<String>,
    val targetBlockId: String,
    val rationale: String,
    val newText: String,
    val title: String,
    val detectedAt: OffsetDateTime,
)
