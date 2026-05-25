package com.docgraph.backend.graph.query.application

fun interface SearchEdgeIdsByTargetDocumentIdsQuery {
    fun search(projectId: Long, targetDocumentIds: List<Long>): List<Long>
}
