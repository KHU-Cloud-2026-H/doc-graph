package com.docgraph.backend.workspace.command.application

data class InviteWorkspaceMemberCommand(
    val workspaceId: Long,
    val requesterUserId: Long,
    val inviteeEmail: String,
)
