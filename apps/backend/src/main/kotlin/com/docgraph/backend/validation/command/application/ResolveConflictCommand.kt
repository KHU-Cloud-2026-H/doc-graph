package com.docgraph.backend.validation.command.application

import java.time.OffsetDateTime

data class ResolveConflictCommand(
    val conflictId: Long,
    val occurredAt: OffsetDateTime,
)
