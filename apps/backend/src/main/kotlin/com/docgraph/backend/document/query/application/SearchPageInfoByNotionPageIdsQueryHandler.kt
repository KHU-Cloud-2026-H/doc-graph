package com.docgraph.backend.document.query.application

import com.docgraph.backend.document.query.infra.DocumentQueryRepository
import org.springframework.stereotype.Service

@Service
class SearchPageInfoByNotionPageIdsQueryHandler(
    private val repo: DocumentQueryRepository,
) : SearchPageInfoByNotionPageIdsQuery {
    override fun search(notionPageIds: List<String>): List<NotionPageRef> =
        repo.findPageInfoByNotionPageIds(notionPageIds)
}
