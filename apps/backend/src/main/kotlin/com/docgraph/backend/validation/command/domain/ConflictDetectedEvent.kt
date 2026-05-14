package com.docgraph.backend.validation.command.domain

import java.time.OffsetDateTime

data class ConflictDetectedEvent(
    val conflictId: Long,
    val edgeId: Long,
    val occurredAt: OffsetDateTime,
)
