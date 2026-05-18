package com.docgraph.backend.project.command.infra

import com.docgraph.backend.project.command.domain.ProjectMember
import org.springframework.data.jpa.repository.JpaRepository

interface ProjectMemberJpaRepository : JpaRepository<ProjectMember, Long> {
    fun findByProjectIdAndWorkspaceMemberId(projectId: Long, workspaceMemberId: Long): ProjectMember?
    fun findAllByProjectId(projectId: Long): List<ProjectMember>
}
