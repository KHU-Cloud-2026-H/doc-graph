package com.docgraph.backend.project.query.application

fun interface SearchProjectRefsByIdsQuery {
    fun search(projectIds: Collection<Long>): List<ProjectRef>
}
