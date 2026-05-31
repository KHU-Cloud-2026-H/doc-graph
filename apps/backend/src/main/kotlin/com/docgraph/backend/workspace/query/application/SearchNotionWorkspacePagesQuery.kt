package com.docgraph.backend.workspace.query.application

fun interface SearchNotionWorkspacePagesQuery {
    fun search(workspaceId: Long, userId: Long, query: String?): List<NotionWorkspacePageResponse>?
}

fun interface SearchNotionRootPagesQuery {
    fun searchRootPages(workspaceId: Long, userId: Long): List<NotionWorkspacePageResponse>?
}

fun interface SearchNotionPageChildrenQuery {
    fun searchPageChildren(workspaceId: Long, userId: Long, pageId: String): List<NotionWorkspacePageResponse>?
}

fun interface FindNotionPageMetadataQuery {
    fun find(workspaceId: Long, userId: Long, pageId: String): NotionWorkspacePageMetadataResponse?
}
