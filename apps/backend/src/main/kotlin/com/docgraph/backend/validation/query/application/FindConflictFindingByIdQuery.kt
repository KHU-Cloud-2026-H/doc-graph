package com.docgraph.backend.validation.query.application

fun interface FindConflictFindingByIdQuery {
    fun find(findingId: Long): ConflictFindingDetail?
}
