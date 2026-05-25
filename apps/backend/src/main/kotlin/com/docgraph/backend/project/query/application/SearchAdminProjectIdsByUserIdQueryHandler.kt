package com.docgraph.backend.project.query.application

import com.docgraph.backend.project.query.infra.ProjectQueryRepository
import org.springframework.stereotype.Service

@Service
class SearchAdminProjectIdsByUserIdQueryHandler(
    private val repo: ProjectQueryRepository,
) : SearchAdminProjectIdsByUserIdQuery {
    override fun search(userId: Long): List<Long> = repo.findAdminProjectIdsByUserId(userId)
}
