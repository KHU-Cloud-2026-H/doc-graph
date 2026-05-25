package com.docgraph.backend.project.command.application

data class DiscardProjectCommand(
    val projectId: Long,
    val requesterUserId: Long,
)
