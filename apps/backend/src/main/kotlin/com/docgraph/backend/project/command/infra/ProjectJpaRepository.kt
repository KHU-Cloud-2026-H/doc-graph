package com.docgraph.backend.project.command.infra

import com.docgraph.backend.project.command.domain.Project
import org.springframework.data.jpa.repository.JpaRepository

interface ProjectJpaRepository : JpaRepository<Project, Long> {
    fun findAllByWorkspaceId(workspaceId: Long): List<Project>
    fun findByWorkspaceIdAndNotionRootPageId(workspaceId: Long, notionRootPageId: String): Project?
}
