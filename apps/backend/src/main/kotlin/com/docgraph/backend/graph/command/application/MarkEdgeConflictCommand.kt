package com.docgraph.backend.graph.command.application

import java.time.OffsetDateTime

data class MarkEdgeConflictCommand(
    val edgeId: Long,
    val occurredAt: OffsetDateTime,
)
