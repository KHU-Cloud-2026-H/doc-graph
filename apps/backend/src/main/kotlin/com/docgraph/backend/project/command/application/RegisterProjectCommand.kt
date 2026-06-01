package com.docgraph.backend.project.command.application

data class RegisterProjectCommand(
    val workspaceId: Long,
    val requesterUserId: Long,
    val name: String,
    val notionRootPageId: String,
    val validationEnabled: Boolean = true,
)
