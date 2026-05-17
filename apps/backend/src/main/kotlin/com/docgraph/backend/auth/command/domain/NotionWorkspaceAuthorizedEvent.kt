package com.docgraph.backend.auth.command.domain

import java.time.OffsetDateTime

data class NotionWorkspaceAuthorizedEvent(
    val ownerUserId: Long,
    val notionWorkspaceId: String,
    val notionWorkspaceName: String,
    val notionBotId: String,
    val occurredAt: OffsetDateTime,
)
