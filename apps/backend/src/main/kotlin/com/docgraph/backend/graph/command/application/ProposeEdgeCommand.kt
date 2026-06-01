package com.docgraph.backend.graph.command.application

data class ProposeEdgeCommand(
    val projectId: Long,
    val sourceDocumentId: Long,
    val targetDocumentId: Long,
    val ruleId: Long?,
    val validationCriterion: String,
    val similarityScore: Double,
)