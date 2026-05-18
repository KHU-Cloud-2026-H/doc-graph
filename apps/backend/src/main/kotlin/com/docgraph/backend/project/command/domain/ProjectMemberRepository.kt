package com.docgraph.backend.project.command.domain

interface ProjectMemberRepository {
    fun save(member: ProjectMember): ProjectMember
    fun findById(id: Long): ProjectMember?
    fun findByProjectIdAndWorkspaceMemberId(projectId: Long, workspaceMemberId: Long): ProjectMember?
    fun findAllByProjectId(projectId: Long): List<ProjectMember>
    fun delete(member: ProjectMember)
}
