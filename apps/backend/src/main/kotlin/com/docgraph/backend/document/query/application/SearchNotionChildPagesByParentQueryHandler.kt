package com.docgraph.backend.document.query.application

import org.springframework.stereotype.Service

@Service
class SearchNotionChildPagesByParentQueryHandler(
    private val notionPageBrowser: NotionPageBrowser,
) : SearchNotionChildPagesByParentQuery {
    override fun search(workspaceId: Long, parentNotionPageId: String): List<NotionPageRef> =
        notionPageBrowser.listChildPages(workspaceId, parentNotionPageId)
}
