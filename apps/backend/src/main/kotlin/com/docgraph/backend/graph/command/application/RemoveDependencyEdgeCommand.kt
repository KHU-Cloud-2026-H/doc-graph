package com.docgraph.backend.graph.command.application

data class RemoveDependencyEdgeCommand(
    val projectId: Long,
    val edgeId: Long,
)
