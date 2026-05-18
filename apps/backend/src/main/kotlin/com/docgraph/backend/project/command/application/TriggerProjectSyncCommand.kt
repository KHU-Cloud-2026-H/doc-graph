package com.docgraph.backend.project.command.application

data class TriggerProjectSyncCommand(
    val projectId: Long,
    val requesterUserId: Long,
)
