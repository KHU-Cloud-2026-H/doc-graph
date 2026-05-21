package com.docgraph.backend.workspace.query.application

import com.docgraph.backend.workspace.query.infra.WorkspaceQueryRepository
import org.springframework.stereotype.Service

@Service
class SearchAccessibleWorkspacesQueryHandler(
    private val queryRepository: WorkspaceQueryRepository,
) : SearchAccessibleWorkspacesQuery {
    override fun search(userId: Long): List<WorkspaceSummary> =
        queryRepository.findAccessibleWorkspaceSummaries(userId)
}