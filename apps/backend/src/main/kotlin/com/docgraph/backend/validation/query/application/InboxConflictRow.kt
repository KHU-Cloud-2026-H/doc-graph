package com.docgraph.backend.validation.query.application

import java.time.OffsetDateTime

data class InboxConflictRow(
    val id: Long,
    val edgeId: Long,
    val firstDetectedAt: OffsetDateTime,
    val ignoredAt: OffsetDateTime?,
)
