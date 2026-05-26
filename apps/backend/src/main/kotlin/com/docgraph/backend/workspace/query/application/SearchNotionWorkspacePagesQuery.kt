package com.docgraph.backend.workspace.query.application

fun interface SearchNotionWorkspacePagesQuery {
    fun search(workspaceId: Long, userId: Long, query: String?): List<NotionWorkspacePageResponse>?
}
