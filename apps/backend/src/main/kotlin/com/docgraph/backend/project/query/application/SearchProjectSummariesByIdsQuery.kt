package com.docgraph.backend.project.query.application

fun interface SearchProjectSummariesByIdsQuery {
    fun search(projectIds: List<Long>): List<ProjectSummary>
}
