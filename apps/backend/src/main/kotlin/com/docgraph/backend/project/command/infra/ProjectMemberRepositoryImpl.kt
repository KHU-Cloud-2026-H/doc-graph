package com.docgraph.backend.project.command.infra

import com.docgraph.backend.project.command.domain.ProjectMember
import com.docgraph.backend.project.command.domain.ProjectMemberRepository
import org.springframework.stereotype.Component

@Component
class ProjectMemberRepositoryImpl(
    private val jpa: ProjectMemberJpaRepository,
) : ProjectMemberRepository {
    override fun save(member: ProjectMember): ProjectMember = jpa.save(member)
    override fun findById(id: Long): ProjectMember? = jpa.findById(id).orElse(null)
    override fun findByProjectIdAndWorkspaceMemberId(projectId: Long, workspaceMemberId: Long): ProjectMember? =
        jpa.findByProjectIdAndWorkspaceMemberId(projectId, workspaceMemberId)
    override fun findAllByProjectId(projectId: Long): List<ProjectMember> =
        jpa.findAllByProjectId(projectId)
    override fun delete(member: ProjectMember) = jpa.delete(member)
}
