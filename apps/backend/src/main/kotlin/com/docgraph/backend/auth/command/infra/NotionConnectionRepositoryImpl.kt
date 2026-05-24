package com.docgraph.backend.auth.command.infra

import com.docgraph.backend.auth.command.domain.NotionConnection
import com.docgraph.backend.auth.command.domain.NotionConnectionRepository
import org.springframework.stereotype.Component

@Component
class NotionConnectionRepositoryImpl(
    private val jpa: NotionConnectionJpaRepository,
) : NotionConnectionRepository {
    override fun save(connection: NotionConnection): NotionConnection = jpa.save(connection)

    override fun findByUserIdAndNotionWorkspaceId(
        userId: Long,
        notionWorkspaceId: String,
    ): NotionConnection? = jpa.findByUserIdAndNotionWorkspaceId(userId, notionWorkspaceId)

    override fun findAllByNotionWorkspaceId(notionWorkspaceId: String): List<NotionConnection> =
        jpa.findAllByNotionWorkspaceId(notionWorkspaceId)
}
