package com.docgraph.backend.workspace.command.infra

import com.docgraph.backend.workspace.command.domain.WorkspaceMember
import org.springframework.data.jpa.repository.JpaRepository

interface WorkspaceMemberJpaRepository : JpaRepository<WorkspaceMember, Long> {
    fun findByWorkspaceIdAndUserId(workspaceId: Long, userId: Long): WorkspaceMember?
    fun findByWorkspaceId(workspaceId: Long): List<WorkspaceMember>
}
