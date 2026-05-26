package com.docgraph.backend.graph.query.application

data class EdgeDetail(
    val id: Long,
    val projectId: Long,
    val sourceDocumentId: Long,
    val targetDocumentId: Long,
    val validationCriterion: String,
)