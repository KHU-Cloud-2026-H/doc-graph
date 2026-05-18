package com.docgraph.backend.workspace.query.application

fun interface FindWorkspaceIdByMemberIdQuery {
    fun find(workspaceMemberId: Long): Long?
}
