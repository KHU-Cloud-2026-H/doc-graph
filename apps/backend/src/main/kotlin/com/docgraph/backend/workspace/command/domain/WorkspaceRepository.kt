package com.docgraph.backend.workspace.command.domain

interface WorkspaceRepository {
    fun save(workspace: Workspace): Workspace
    fun findById(id: Long): Workspace?
    fun findByNotionWorkspaceId(notionWorkspaceId: String): Workspace?
    fun delete(workspace: Workspace)
}
