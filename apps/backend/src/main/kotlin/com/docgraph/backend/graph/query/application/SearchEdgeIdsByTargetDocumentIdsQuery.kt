package com.docgraph.backend.graph.query.application

fun interface SearchEdgeIdsByTargetDocumentIdsQuery {
    fun search(targetDocumentIds: List<Long>): List<Long>
}
