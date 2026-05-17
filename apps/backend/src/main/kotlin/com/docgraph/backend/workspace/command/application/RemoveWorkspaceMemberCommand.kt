package com.docgraph.backend.workspace.command.application

data class RemoveWorkspaceMemberCommand(
    val workspaceId: Long,
    val requesterUserId: Long,
    val userId: Long,
)
