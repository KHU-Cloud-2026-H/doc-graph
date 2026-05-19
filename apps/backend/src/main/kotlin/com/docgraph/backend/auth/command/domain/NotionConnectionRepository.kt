package com.docgraph.backend.auth.command.domain

interface NotionConnectionRepository {
    fun save(connection: NotionConnection): NotionConnection
    fun findByUserIdAndNotionWorkspaceId(userId: Long, notionWorkspaceId: String): NotionConnection?
    fun findAllByNotionWorkspaceId(notionWorkspaceId: String): List<NotionConnection>
}
