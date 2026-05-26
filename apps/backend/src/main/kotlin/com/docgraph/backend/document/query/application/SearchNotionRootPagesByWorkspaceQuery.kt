package com.docgraph.backend.document.query.application

fun interface SearchNotionRootPagesByWorkspaceQuery {
    fun search(workspaceId: Long): List<NotionPageRef>
}
