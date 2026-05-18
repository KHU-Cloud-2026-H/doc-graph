package com.docgraph.backend.project.command.domain

import java.time.OffsetDateTime

data class ProjectSyncTriggeredEvent(
    val projectId: Long,
    val triggeredBy: Long,
    val occurredAt: OffsetDateTime = OffsetDateTime.now(),
)
