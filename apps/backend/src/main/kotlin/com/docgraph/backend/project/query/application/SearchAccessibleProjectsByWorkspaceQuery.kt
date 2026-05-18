package com.docgraph.backend.project.query.application

fun interface SearchAccessibleProjectsByWorkspaceQuery {
    fun search(workspaceId: Long, userId: Long): List<ProjectSummary>
}
