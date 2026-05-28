package com.docgraph.backend.document.query.application

fun interface SearchNotionChildPagesByParentQuery {
    fun search(workspaceId: Long, parentNotionPageId: String): List<NotionPageRef>
}
