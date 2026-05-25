package com.docgraph.backend.workspace.query.application

fun interface SearchAccessibleWorkspacesQuery {
    fun search(userId: Long): List<WorkspaceSummary>
}