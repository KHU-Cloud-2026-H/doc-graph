package com.docgraph.backend.workspace.command.infra

import com.docgraph.backend.workspace.command.domain.Workspace
import com.docgraph.backend.workspace.command.domain.WorkspaceRepository
import org.springframework.stereotype.Component

@Component
class WorkspaceRepositoryImpl(
    private val jpa: WorkspaceJpaRepository,
) : WorkspaceRepository {
    override fun save(workspace: Workspace): Workspace = jpa.save(workspace)
    override fun findById(id: Long): Workspace? = jpa.findById(id).orElse(null)
    override fun findByNotionWorkspaceId(notionWorkspaceId: String): Workspace? =
        jpa.findByNotionWorkspaceId(notionWorkspaceId)
    override fun delete(workspace: Workspace) = jpa.delete(workspace)
}
