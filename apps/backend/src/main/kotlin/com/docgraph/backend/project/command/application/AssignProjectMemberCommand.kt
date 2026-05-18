package com.docgraph.backend.project.command.application

import com.docgraph.backend.project.command.domain.ProjectMemberRole

data class AssignProjectMemberCommand(
    val projectId: Long,
    val requesterUserId: Long,
    val workspaceMemberId: Long,
    val role: ProjectMemberRole,
)
