package com.docgraph.backend.workspace.command.domain

interface WorkspaceMemberRepository {
    fun save(member: WorkspaceMember): WorkspaceMember
    fun findById(id: Long): WorkspaceMember?
    fun findByWorkspaceIdAndUserId(workspaceId: Long, userId: Long): WorkspaceMember?
    fun findByWorkspaceId(workspaceId: Long): List<WorkspaceMember>
    fun delete(member: WorkspaceMember)
}
