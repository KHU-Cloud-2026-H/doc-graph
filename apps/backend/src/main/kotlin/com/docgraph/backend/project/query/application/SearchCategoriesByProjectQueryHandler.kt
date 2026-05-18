package com.docgraph.backend.project.query.application

import com.docgraph.backend.project.query.infra.ProjectQueryRepository
import org.springframework.stereotype.Service

@Service
class SearchCategoriesByProjectQueryHandler(
    private val repo: ProjectQueryRepository,
) : SearchCategoriesByProjectQuery {
    override fun search(projectId: Long): List<CategoryProjection> = repo.findCategoriesByProjectId(projectId)
}
