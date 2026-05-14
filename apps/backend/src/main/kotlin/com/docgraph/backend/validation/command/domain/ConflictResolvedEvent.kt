package com.docgraph.backend.validation.command.domain

import java.time.OffsetDateTime

data class ConflictResolvedEvent(
    val conflictId: Long,
    val edgeId: Long,
    val occurredAt: OffsetDateTime,
)
