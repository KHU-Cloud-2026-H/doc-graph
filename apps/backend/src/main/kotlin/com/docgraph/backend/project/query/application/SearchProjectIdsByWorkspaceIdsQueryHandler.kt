package com.docgraph.backend.project.query.application

import com.docgraph.backend.project.query.infra.ProjectQueryRepository
import org.springframework.stereotype.Service

@Service
class SearchProjectIdsByWorkspaceIdsQueryHandler(
    private val repo: ProjectQueryRepository,
) : SearchProjectIdsByWorkspaceIdsQuery {
    override fun search(workspaceIds: Collection<Long>): Map<Long, List<Long>> =
        repo.findProjectIdsByWorkspaceIdIn(workspaceIds)
}
