package com.docgraph.backend.project.query.application

fun interface SearchProjectIdsByWorkspaceIdsQuery {
    fun search(workspaceIds: Collection<Long>): Map<Long, List<Long>>
}
