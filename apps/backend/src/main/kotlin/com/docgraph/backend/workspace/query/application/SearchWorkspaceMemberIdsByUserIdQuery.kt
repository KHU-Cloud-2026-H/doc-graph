package com.docgraph.backend.workspace.query.application

fun interface SearchWorkspaceMemberIdsByUserIdQuery {
    fun search(userId: Long): List<Long>
}
