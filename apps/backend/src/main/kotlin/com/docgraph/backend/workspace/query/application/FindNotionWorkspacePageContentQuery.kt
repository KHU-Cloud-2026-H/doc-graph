package com.docgraph.backend.workspace.query.application

fun interface FindNotionWorkspacePageContentQuery {
    fun find(workspaceId: Long, userId: Long, pageId: String): NotionWorkspacePageContentResponse?
}
