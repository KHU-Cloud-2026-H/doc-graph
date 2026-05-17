package com.docgraph.backend.validation.query.application

import java.time.OffsetDateTime

data class ConflictFindingRow(
    val id: Long,
    val conflictId: Long,
    val sourceBlockIds: List<String>,
    val targetBlockIds: List<String>,
    val rationale: String,
    val suggestion: String,
    val detectedAt: OffsetDateTime,
)
