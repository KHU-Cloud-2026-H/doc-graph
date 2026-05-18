package com.docgraph.backend.project.command.application

data class RemoveProjectMemberCommand(
    val projectId: Long,
    val requesterUserId: Long,
    val memberId: Long,
)
