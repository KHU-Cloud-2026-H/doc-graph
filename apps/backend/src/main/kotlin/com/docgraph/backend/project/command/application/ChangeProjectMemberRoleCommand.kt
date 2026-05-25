package com.docgraph.backend.project.command.application

import com.docgraph.backend.project.command.domain.ProjectMemberRole

data class ChangeProjectMemberRoleCommand(
    val projectId: Long,
    val requesterUserId: Long,
    val memberId: Long,
    val role: ProjectMemberRole,
)
