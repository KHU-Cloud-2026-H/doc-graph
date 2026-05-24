package com.docgraph.backend.document.command.domain

import java.time.OffsetDateTime

data class NotionWriteSucceededEvent(
    val conflictFindingId: Long,
    val occurredAt: OffsetDateTime,
)
