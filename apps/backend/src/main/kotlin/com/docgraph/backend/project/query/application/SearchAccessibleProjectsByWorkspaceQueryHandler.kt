package com.docgraph.backend.project.query.application

import com.docgraph.backend.project.query.infra.ProjectQueryRepository
import org.springframework.stereotype.Service

@Service
class SearchAccessibleProjectsByWorkspaceQueryHandler(
    private val repo: ProjectQueryRepository,
) : SearchAccessibleProjectsByWorkspaceQuery {
    override fun search(workspaceId: Long, userId: Long): List<ProjectSummary> =
        repo.findAccessibleProjectSummaries(workspaceId, userId)
}
