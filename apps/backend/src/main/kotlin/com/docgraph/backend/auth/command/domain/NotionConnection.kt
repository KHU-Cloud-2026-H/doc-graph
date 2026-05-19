package com.docgraph.backend.auth.command.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "notion_connection")
class NotionConnection(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "notion_workspace_id", nullable = false, length = 100)
    val notionWorkspaceId: String,

    @Column(name = "notion_workspace_name", nullable = false, length = 500)
    var notionWorkspaceName: String,

    @Column(name = "notion_bot_id", nullable = false, length = 100)
    var notionBotId: String,

    @Column(name = "access_token_encrypted", nullable = false, columnDefinition = "TEXT")
    var accessTokenEncrypted: String,

    @Column(name = "token_type", nullable = false, length = 50)
    var tokenType: String,

    @Column(name = "connected_at", nullable = false)
    val connectedAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "revoked_at")
    var revokedAt: OffsetDateTime? = null,
) {
    init {
        require(notionWorkspaceId.isNotBlank()) { "notionWorkspaceId must not be blank" }
        require(notionWorkspaceName.isNotBlank()) { "notionWorkspaceName must not be blank" }
        require(notionBotId.isNotBlank()) { "notionBotId must not be blank" }
        require(accessTokenEncrypted.isNotBlank()) { "accessTokenEncrypted must not be blank" }
        require(tokenType.isNotBlank()) { "tokenType must not be blank" }
    }

    fun rotateToken(accessTokenEncrypted: String, tokenType: String) {
        require(accessTokenEncrypted.isNotBlank()) { "accessTokenEncrypted must not be blank" }
        require(tokenType.isNotBlank()) { "tokenType must not be blank" }
        this.accessTokenEncrypted = accessTokenEncrypted
        this.tokenType = tokenType
        this.revokedAt = null
    }

    fun updateWorkspace(notionWorkspaceName: String, notionBotId: String) {
        require(notionWorkspaceName.isNotBlank()) { "notionWorkspaceName must not be blank" }
        require(notionBotId.isNotBlank()) { "notionBotId must not be blank" }
        this.notionWorkspaceName = notionWorkspaceName
        this.notionBotId = notionBotId
    }

    fun revoke(at: OffsetDateTime = OffsetDateTime.now()) {
        revokedAt = at
    }

    companion object {
        fun connect(
            userId: Long,
            notionWorkspaceId: String,
            notionWorkspaceName: String,
            notionBotId: String,
            accessTokenEncrypted: String,
            tokenType: String,
            now: OffsetDateTime = OffsetDateTime.now(),
        ): NotionConnection = NotionConnection(
            userId = userId,
            notionWorkspaceId = notionWorkspaceId,
            notionWorkspaceName = notionWorkspaceName,
            notionBotId = notionBotId,
            accessTokenEncrypted = accessTokenEncrypted,
            tokenType = tokenType,
            connectedAt = now,
        )
    }
}
