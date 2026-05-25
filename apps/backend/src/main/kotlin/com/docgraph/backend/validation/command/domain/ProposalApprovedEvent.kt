package com.docgraph.backend.validation.command.domain

import java.time.OffsetDateTime

data class ProposalApprovedEvent(
    val conflictFindingId: Long,
    val approvedBy: Long,
    val occurredAt: OffsetDateTime,
)
