package com.docgraph.backend.workspace.query.application

import com.docgraph.backend.workspace.command.domain.WorkspaceMemberRepository
import org.springframework.stereotype.Service

@Service
class FindWorkspaceMemberIdByUserIdQueryHandler(
    private val workspaceMemberRepository: WorkspaceMemberRepository,
) : FindWorkspaceMemberIdByUserIdQuery {
    override fun find(workspaceId: Long, userId: Long): Long? =
        workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)?.id
}
