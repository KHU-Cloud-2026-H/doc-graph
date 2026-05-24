package com.docgraph.backend.graph.command.application

import com.docgraph.backend.graph.command.domain.DependencyEdgeSource
import java.util.UUID

data class CreateDependencyEdgeCommand(
    val projectId: Long,
    val sourceDocumentId: Long,
    val targetDocumentId: Long,
    val ruleId: Long?,
    val validationCriterion: String,
    val source: DependencyEdgeSource,
    val validationPairId: UUID = UUID.randomUUID(),
)
