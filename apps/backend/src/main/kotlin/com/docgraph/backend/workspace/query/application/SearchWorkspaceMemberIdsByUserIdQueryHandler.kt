package com.docgraph.backend.workspace.query.application

import com.docgraph.backend.workspace.query.infra.WorkspaceQueryRepository
import org.springframework.stereotype.Service

@Service
class SearchWorkspaceMemberIdsByUserIdQueryHandler(
    private val repo: WorkspaceQueryRepository,
) : SearchWorkspaceMemberIdsByUserIdQuery {
    override fun search(userId: Long): List<Long> = repo.findMemberIdsByUserId(userId)
}
