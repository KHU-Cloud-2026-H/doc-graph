package com.docgraph.backend.graph.query.application

fun interface SearchEdgeIdsByProjectQuery {
    fun search(projectId: Long): List<Long>
}