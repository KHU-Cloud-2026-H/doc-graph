package com.docgraph.backend.document.command.domain

import java.time.OffsetDateTime

interface NotionDocumentClient {
    fun fetchPage(pageId: String, accessToken: String? = null): NotionPage
    fun fetchBlockChildren(blockId: String, accessToken: String? = null): List<NotionBlock>
    fun searchPages(accessToken: String, query: String? = null, pageSize: Int = 50): List<NotionSearchPage>
}

data class NotionPage(
    val id: String,
    val title: String,
    val createdTime: OffsetDateTime?,
    val lastEditedTime: OffsetDateTime?,
    val createdBy: String?,
    val lastEditedBy: String?,
    val rawJson: String,
)

data class NotionBlock(
    val id: String,
    val type: String,
    val parentType: String?,
    val parentId: String?,
    val text: String?,
    val linkedPageIds: Set<String>,
    val childPageTitle: String?,
    val createdTime: OffsetDateTime?,
    val lastEditedTime: OffsetDateTime?,
    val createdBy: String?,
    val lastEditedBy: String?,
    val hasChildren: Boolean,
    val archived: Boolean,
    val inTrash: Boolean,
    val rawJson: String,
)

data class NotionSearchPage(
    val id: String,
    val title: String,
    val url: String?,
    val lastEditedTime: OffsetDateTime?,
)
