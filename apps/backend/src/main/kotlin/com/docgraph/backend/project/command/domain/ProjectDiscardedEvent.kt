package com.docgraph.backend.project.command.domain

import java.time.OffsetDateTime

data class ProjectDiscardedEvent(
    val projectId: Long,
    val occurredAt: OffsetDateTime = OffsetDateTime.now(),
)
