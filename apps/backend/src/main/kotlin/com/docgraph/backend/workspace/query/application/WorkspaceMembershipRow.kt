package com.docgraph.backend.workspace.query.application

import java.time.OffsetDateTime

data class WorkspaceMembershipRow(
    val userId: Long,
    val joinedAt: OffsetDateTime,
)