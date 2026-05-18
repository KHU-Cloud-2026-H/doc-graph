package com.docgraph.backend.workspace.query.application

import com.docgraph.backend.workspace.command.infra.WorkspaceMemberJpaRepository
import org.springframework.stereotype.Service

@Service
class FindWorkspaceIdByMemberIdQueryHandler(
    private val jpa: WorkspaceMemberJpaRepository,
) : FindWorkspaceIdByMemberIdQuery {
    override fun find(workspaceMemberId: Long): Long? =
        jpa.findById(workspaceMemberId).orElse(null)?.workspaceId
}
