package com.docgraph.backend.graph.query.application

fun interface SearchEdgeIdsByProjectIdsQuery {
    fun search(projectIds: Collection<Long>): Map<Long, List<Long>>
}
