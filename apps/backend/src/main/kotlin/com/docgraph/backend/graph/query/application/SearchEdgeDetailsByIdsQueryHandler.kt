package com.docgraph.backend.graph.query.application

import com.docgraph.backend.graph.command.domain.DependencyEdgeRepository
import org.springframework.stereotype.Service

@Service
class SearchEdgeDetailsByIdsQueryHandler(
    private val edgeRepository: DependencyEdgeRepository,
) : SearchEdgeDetailsByIdsQuery {
    override fun search(edgeIds: List<Long>): List<EdgeDetail> =
        edgeRepository.findAllByIds(edgeIds).map { it.toDetail() }
}
