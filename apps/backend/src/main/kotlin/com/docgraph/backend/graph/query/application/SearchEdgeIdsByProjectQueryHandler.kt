package com.docgraph.backend.graph.query.application

import com.docgraph.backend.graph.command.domain.DependencyEdgeRepository
import org.springframework.stereotype.Service

@Service
class SearchEdgeIdsByProjectQueryHandler(
    private val edgeRepository: DependencyEdgeRepository,
) : SearchEdgeIdsByProjectQuery {
    override fun search(projectId: Long): List<Long> =
        edgeRepository.findAllByProjectId(projectId).map { it.id }
}
