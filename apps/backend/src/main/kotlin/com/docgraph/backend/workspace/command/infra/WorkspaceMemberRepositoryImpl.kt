package com.docgraph.backend.workspace.command.infra

import com.docgraph.backend.workspace.command.domain.WorkspaceMember
import com.docgraph.backend.workspace.command.domain.WorkspaceMemberRepository
import org.springframework.stereotype.Component

@Component
class WorkspaceMemberRepositoryImpl(
    private val jpa: WorkspaceMemberJpaRepository,
) : WorkspaceMemberRepository {
    override fun save(member: WorkspaceMember): WorkspaceMember = jpa.save(member)
    override fun findById(id: Long): WorkspaceMember? = jpa.findById(id).orElse(null)
    override fun findByWorkspaceIdAndUserId(workspaceId: Long, userId: Long): WorkspaceMember? =
        jpa.findByWorkspaceIdAndUserId(workspaceId, userId)
    override fun findByWorkspaceId(workspaceId: Long): List<WorkspaceMember> =
        jpa.findByWorkspaceId(workspaceId)
    override fun delete(member: WorkspaceMember) = jpa.delete(member)
}
