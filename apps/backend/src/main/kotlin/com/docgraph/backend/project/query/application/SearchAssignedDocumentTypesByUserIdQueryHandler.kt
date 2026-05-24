package com.docgraph.backend.project.query.application

import com.docgraph.backend.project.query.infra.ProjectQueryRepository
import org.springframework.stereotype.Service

@Service
class SearchAssignedDocumentTypesByUserIdQueryHandler(
    private val repo: ProjectQueryRepository,
) : SearchAssignedDocumentTypesByUserIdQuery {
    override fun search(userId: Long): List<AssignedDocumentType> =
        repo.findAssignedDocumentTypesByUserId(userId)
}
