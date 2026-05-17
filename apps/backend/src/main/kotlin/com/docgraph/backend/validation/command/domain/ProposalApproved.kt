package com.docgraph.backend.validation.command.domain

import java.time.OffsetDateTime

data class ProposalApproved(
    val conflictFindingId: Long,
    val approvedBy: Long,
    val occurredAt: OffsetDateTime,
)
