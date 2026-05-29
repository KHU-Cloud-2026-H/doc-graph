package com.docgraph.backend.validation.query.application

fun interface CountUnresolvedConflictsByProjectIdsQuery {
    fun count(projectIds: Collection<Long>): Map<Long, Long>
}
