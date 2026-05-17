package com.docgraph.backend.validation.command.domain

data class DetectedConflict(
    val sourceBlockIds: List<String>,
    val targetBlockIds: List<String>,
    val rationale: String,
    val suggestion: String,
)