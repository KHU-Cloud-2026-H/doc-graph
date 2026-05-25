package com.docgraph.backend.auth.command.infra

import com.docgraph.backend.auth.command.domain.NotionConnection
import org.springframework.data.jpa.repository.JpaRepository

interface NotionConnectionJpaRepository : JpaRepository<NotionConnection, Long> {
    fun findByUserIdAndNotionWorkspaceId(userId: Long, notionWorkspaceId: String): NotionConnection?
    fun findAllByNotionWorkspaceId(notionWorkspaceId: String): List<NotionConnection>
}
