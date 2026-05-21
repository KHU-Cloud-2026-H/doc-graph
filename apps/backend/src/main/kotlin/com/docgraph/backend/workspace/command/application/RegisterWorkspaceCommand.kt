package com.docgraph.backend.workspace.command.application

data class RegisterWorkspaceCommand(
    val ownerUserId: Long,
    val notionWorkspaceId: String,
    val notionWorkspaceName: String,
    val notionBotId: String,
)
