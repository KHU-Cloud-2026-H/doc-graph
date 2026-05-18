package com.docgraph.backend.workspace.query.application

fun interface FindWorkspaceMemberIdByUserIdQuery {
    fun find(workspaceId: Long, userId: Long): Long?
}
