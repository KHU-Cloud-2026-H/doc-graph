package com.docgraph.backend.project.query.application

import com.docgraph.backend.project.query.infra.ProjectQueryRepository
import org.springframework.stereotype.Service

@Service
class SearchTypeAssigneeDefaultsByProjectQueryHandler(
    private val repo: ProjectQueryRepository,
) : SearchTypeAssigneeDefaultsByProjectQuery {
    override fun search(projectId: Long, userId: Long): List<TypeAssigneeResponse>? {
        repo.findProjectIfAccessible(projectId, userId) ?: return null
        return repo.findTypeAssigneesByProjectId(projectId)
    }
}
