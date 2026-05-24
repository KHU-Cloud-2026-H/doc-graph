package com.docgraph.backend.project.query.application

import com.docgraph.backend.project.query.infra.ProjectQueryRepository
import org.springframework.stereotype.Service

@Service
class SearchProjectSummariesByIdsQueryHandler(
    private val repo: ProjectQueryRepository,
) : SearchProjectSummariesByIdsQuery {
    override fun search(projectIds: List<Long>): List<ProjectSummary> =
        repo.findSummariesByIds(projectIds)
}
