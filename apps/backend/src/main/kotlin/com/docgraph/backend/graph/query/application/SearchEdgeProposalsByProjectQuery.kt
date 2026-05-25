package com.docgraph.backend.graph.query.application

fun interface SearchEdgeProposalsByProjectQuery {
    fun search(projectId: Long): List<EdgeProposalResponse>
}
