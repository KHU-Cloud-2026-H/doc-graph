package com.docgraph.backend.workspace.command.infra

import com.docgraph.backend.workspace.command.domain.Workspace
import org.springframework.data.jpa.repository.JpaRepository

interface WorkspaceJpaRepository : JpaRepository<Workspace, Long> {
    fun findByNotionWorkspaceId(notionWorkspaceId: String): Workspace?
}
