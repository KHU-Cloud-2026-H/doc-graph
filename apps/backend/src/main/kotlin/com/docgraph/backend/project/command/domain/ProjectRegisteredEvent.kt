package com.docgraph.backend.project.command.domain

import java.time.OffsetDateTime

data class ProjectRegisteredEvent(
    val projectId: Long,
    val validationEnabled: Boolean,
    val occurredAt: OffsetDateTime = OffsetDateTime.now(),
)
