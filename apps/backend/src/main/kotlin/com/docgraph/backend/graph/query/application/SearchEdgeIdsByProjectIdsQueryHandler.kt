package com.docgraph.backend.graph.query.application

import com.docgraph.backend.graph.query.infra.GraphQueryRepository
import org.springframework.stereotype.Service

@Service
class SearchEdgeIdsByProjectIdsQueryHandler(
    private val graphQueryRepository: GraphQueryRepository,
) : SearchEdgeIdsByProjectIdsQuery {
    override fun search(projectIds: Collection<Long>): Map<Long, List<Long>> =
        graphQueryRepository.findEdgeIdsByProjectIdIn(projectIds)
}