package com.docgraph.backend.validation.query.application

import java.time.OffsetDateTime

data class MyConflictRow(
    val id: Long,
    val edgeId: Long,
    val projectId: Long,
    val projectName: String,
    val sourceDocument: InboxDocumentRef,
    val targetDocument: InboxDocumentRef,
    val status: MyConflictStatus,
    val firstDetectedAt: OffsetDateTime,
    val ignoredAt: OffsetDateTime?,
)
