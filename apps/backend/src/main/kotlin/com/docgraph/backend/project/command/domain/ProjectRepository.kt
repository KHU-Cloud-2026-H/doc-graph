package com.docgraph.backend.project.command.domain

interface ProjectRepository {
    fun save(project: Project): Project
    fun findById(id: Long): Project?
    fun findByWorkspaceIdAndNotionRootPageId(workspaceId: Long, notionRootPageId: String): Project?
    fun findAllByWorkspaceId(workspaceId: Long): List<Project>
    fun delete(project: Project)
}
