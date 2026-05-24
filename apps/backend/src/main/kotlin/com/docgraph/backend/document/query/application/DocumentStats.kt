package com.docgraph.backend.document.query.application

import java.time.OffsetDateTime

data class DocumentStats(
    val documentCount: Long,
    val lastNotionChangedAt: OffsetDateTime?,
)
