package com.docgraph.backend.graph.command.application

import com.docgraph.backend.document.query.application.DocumentType

data class RegisterGraphRuleCommand(
    val projectId: Long,
    val sourceType: DocumentType,
    val targetType: DocumentType,
    val validationCriterion: String,
)
