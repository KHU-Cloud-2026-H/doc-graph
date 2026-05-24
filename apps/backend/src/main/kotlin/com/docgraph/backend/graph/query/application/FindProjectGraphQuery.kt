package com.docgraph.backend.graph.query.application

fun interface FindProjectGraphQuery {
    fun find(projectId: Long): ProjectGraphResponse
}
