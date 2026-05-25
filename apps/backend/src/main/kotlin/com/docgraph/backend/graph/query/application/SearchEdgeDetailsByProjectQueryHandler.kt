package com.docgraph.backend.graph.query.application

import com.docgraph.backend.graph.command.domain.DependencyEdgeRepository
import org.springframework.stereotype.Service

@Service
class SearchEdgeDetailsByProjectQueryHandler(
    private val edgeRepository: DependencyEdgeRepository,
) : SearchEdgeDetailsByProjectQuery {
    override fun search(projectId: Long): List<EdgeDetail> =
        edgeRepository.findAllByProjectId(projectId).map { it.toDetail() }
}
