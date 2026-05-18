package com.docgraph.backend.validation.query.application

import com.docgraph.backend.event.OutboxStatus
import java.time.OffsetDateTime

data class ValidationTaskRow(
    val id: Long,
    val edgeId: Long,
    val status: OutboxStatus,
    val createdAt: OffsetDateTime,
)
