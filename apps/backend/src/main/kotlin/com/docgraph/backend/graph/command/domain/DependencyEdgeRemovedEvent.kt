package com.docgraph.backend.graph.command.domain

import java.time.OffsetDateTime

data class DependencyEdgeRemovedEvent(
    val edgeId: Long,
    val projectId: Long,
    val occurredAt: OffsetDateTime,
)
