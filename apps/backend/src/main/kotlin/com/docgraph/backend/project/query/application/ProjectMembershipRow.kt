package com.docgraph.backend.project.query.application

import com.docgraph.backend.project.command.domain.ProjectMemberRole

data class ProjectMembershipRow(
    val id: Long,
    val userId: Long,
    val role: ProjectMemberRole,
)
