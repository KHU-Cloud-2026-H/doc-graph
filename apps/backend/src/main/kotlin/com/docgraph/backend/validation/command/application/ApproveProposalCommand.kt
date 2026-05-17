package com.docgraph.backend.validation.command.application

import java.time.OffsetDateTime

data class ApproveProposalCommand(
    val findingId: Long,
    val approvedBy: Long,
    val expectedTargetNotionLastEditedAt: OffsetDateTime?,
)
