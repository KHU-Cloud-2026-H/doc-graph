package com.docgraph.backend.workspace.query.application

fun interface FindWorkspaceCreatorIdByIdQuery {
    fun find(workspaceId: Long): Long?
}
