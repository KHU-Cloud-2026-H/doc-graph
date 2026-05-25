package com.docgraph.backend.validation.query.application

import com.docgraph.backend.graph.query.application.SearchEdgeIdsByProjectIdsQuery
import com.docgraph.backend.validation.query.infra.ValidationQueryRepository
import org.springframework.stereotype.Service

@Service
class CountUnresolvedConflictsByProjectIdsQueryHandler(
    private val searchEdgeIdsByProjects: SearchEdgeIdsByProjectIdsQuery,
    private val repository: ValidationQueryRepository,
) : CountUnresolvedConflictsByProjectIdsQuery {

    override fun count(projectIds: Collection<Long>): Map<Long, Long> {
        if (projectIds.isEmpty()) return emptyMap()

        val projectEdges: Map<Long, List<Long>> = searchEdgeIdsByProjects.search(projectIds)
        if (projectEdges.isEmpty()) return emptyMap()

        val allEdgeIds: Set<Long> = projectEdges.values.flatten().toSet()
        if (allEdgeIds.isEmpty()) return emptyMap()

        val activeEdgeIds: Set<Long> = repository.findActiveUnignoredEdgeIds(allEdgeIds).toSet()

        return projectEdges.mapValues { (_, edges) ->
            edges.count { it in activeEdgeIds }.toLong()
        }
    }
}
