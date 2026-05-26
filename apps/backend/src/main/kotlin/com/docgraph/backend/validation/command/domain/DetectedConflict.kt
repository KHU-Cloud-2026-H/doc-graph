package com.docgraph.backend.validation.command.domain

data class DetectedConflict(
    val sourceBlockIds: List<String>,
    val targetBlockId: String,
    val rationale: String,
    val newText: String,
    val title: String,
)