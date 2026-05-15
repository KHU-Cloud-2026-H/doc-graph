package com.docgraph.backend.graph.query.application

fun interface SearchEdgeDetailsByProjectQuery {
    fun search(projectId: Long): List<EdgeDetail>
}