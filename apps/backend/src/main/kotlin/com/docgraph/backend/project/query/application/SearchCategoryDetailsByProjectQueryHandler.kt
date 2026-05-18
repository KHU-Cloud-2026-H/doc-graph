package com.docgraph.backend.project.query.application

import com.docgraph.backend.project.query.infra.ProjectQueryRepository
import org.springframework.stereotype.Service

@Service
class SearchCategoryDetailsByProjectQueryHandler(
    private val repo: ProjectQueryRepository,
) : SearchCategoryDetailsByProjectQuery {
    override fun search(projectId: Long, userId: Long): List<CategoryResponse>? {
        repo.findProjectIfAccessible(projectId, userId) ?: return null
        return repo.findCategoryDetailsByProjectId(projectId)
    }
}
