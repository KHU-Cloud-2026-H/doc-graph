package com.docgraph.backend.project.query.application

import com.docgraph.backend.project.query.infra.ProjectQueryRepository
import org.springframework.stereotype.Service

@Service
class SearchProjectRefsByIdsQueryHandler(
    private val repo: ProjectQueryRepository,
) : SearchProjectRefsByIdsQuery {
    override fun search(projectIds: Collection<Long>): List<ProjectRef> =
        repo.findProjectRefsByIds(projectIds)
}
