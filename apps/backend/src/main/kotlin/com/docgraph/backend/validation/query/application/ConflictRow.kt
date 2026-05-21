package com.docgraph.backend.validation.query.application

import java.time.OffsetDateTime

data class ConflictRow(
    val id: Long,
    val edgeId: Long,
    val ignoredAt: OffsetDateTime?,
    val ignoreReason: String?,
)
