package com.docgraph.backend.workspace.query.application

fun interface FindWorkspaceDetailByIdQuery {
    fun find(workspaceId: Long, userId: Long): WorkspaceDetail?
}