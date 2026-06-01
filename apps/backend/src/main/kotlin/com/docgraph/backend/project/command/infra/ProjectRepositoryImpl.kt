package com.docgraph.backend.project.command.infra

import com.docgraph.backend.project.command.domain.Project
import com.docgraph.backend.project.command.domain.ProjectRepository
import org.springframework.stereotype.Component

@Component
class ProjectRepositoryImpl(
    private val jpa: ProjectJpaRepository,
) : ProjectRepository {
    override fun save(project: Project): Project = jpa.save(project)
    override fun findById(id: Long): Project? = jpa.findById(id).orElse(null)
    override fun findByWorkspaceIdAndNotionRootPageId(workspaceId: Long, notionRootPageId: String): Project? =
        jpa.findByWorkspaceIdAndNotionRootPageId(workspaceId, notionRootPageId)
    override fun findAllByWorkspaceId(workspaceId: Long): List<Project> = jpa.findAllByWorkspaceId(workspaceId)
    override fun delete(project: Project) = jpa.delete(project)
}
