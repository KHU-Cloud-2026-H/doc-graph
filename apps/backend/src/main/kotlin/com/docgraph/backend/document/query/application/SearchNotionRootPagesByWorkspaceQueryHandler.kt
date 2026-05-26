package com.docgraph.backend.document.query.application

import org.springframework.stereotype.Service

@Service
class SearchNotionRootPagesByWorkspaceQueryHandler(
    private val notionPageBrowser: NotionPageBrowser,
) : SearchNotionRootPagesByWorkspaceQuery {
    override fun search(workspaceId: Long): List<NotionPageRef> =
        notionPageBrowser.listRootPages(workspaceId)
}
