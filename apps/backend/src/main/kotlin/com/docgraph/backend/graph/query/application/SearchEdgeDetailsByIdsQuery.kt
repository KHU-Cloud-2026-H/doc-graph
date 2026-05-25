package com.docgraph.backend.graph.query.application

fun interface SearchEdgeDetailsByIdsQuery {
    fun search(edgeIds: List<Long>): List<EdgeDetail>
}
