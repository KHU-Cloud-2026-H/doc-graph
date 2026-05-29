package com.docgraph.backend.document.query.application

fun interface SearchPageInfoByNotionPageIdsQuery {
    fun search(notionPageIds: List<String>): List<NotionPageRef>
}
