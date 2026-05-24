package com.docgraph.backend.graph.query.application

fun interface SearchEdgesByProjectQuery {
    fun search(projectId: Long): List<EdgeResponse>
}
