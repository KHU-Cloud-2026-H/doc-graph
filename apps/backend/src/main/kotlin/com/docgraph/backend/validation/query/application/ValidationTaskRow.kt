package com.docgraph.backend.validation.query.application

import com.docgraph.backend.event.OutboxStatus
import com.docgraph.backend.validation.command.domain.FailureCategory
import java.time.OffsetDateTime

data class ValidationTaskRow(
    val id: Long,
    val edgeId: Long,
    val status: OutboxStatus,
    val failureCategory: FailureCategory?,
    val createdAt: OffsetDateTime,
)
