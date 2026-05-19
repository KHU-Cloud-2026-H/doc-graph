package com.docgraph.backend.auth.command.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

@Tag("unit")
class NotionConnectionTest {

    @Test
    fun `connect — Notion 워크스페이스 연결 생성`() {
        val now = OffsetDateTime.parse("2026-05-20T10:00:00+09:00")

        val connection = NotionConnection.connect(
            userId = 1L,
            notionWorkspaceId = "workspace-1",
            notionWorkspaceName = "Workspace",
            notionBotId = "bot-1",
            accessTokenEncrypted = "encrypted-token",
            tokenType = "bearer",
            now = now,
        )

        assertEquals(1L, connection.userId)
        assertEquals("workspace-1", connection.notionWorkspaceId)
        assertEquals("Workspace", connection.notionWorkspaceName)
        assertEquals("bot-1", connection.notionBotId)
        assertEquals("encrypted-token", connection.accessTokenEncrypted)
        assertEquals("bearer", connection.tokenType)
        assertEquals(now, connection.connectedAt)
        assertNull(connection.revokedAt)
    }

    @Test
    fun `rotateToken — 토큰 갱신 시 revoke 상태 해제`() {
        val connection = NotionConnection.connect(
            userId = 1L,
            notionWorkspaceId = "workspace-1",
            notionWorkspaceName = "Workspace",
            notionBotId = "bot-1",
            accessTokenEncrypted = "old-token",
            tokenType = "bearer",
        )
        connection.revoke(OffsetDateTime.parse("2026-05-20T10:00:00+09:00"))

        connection.rotateToken(accessTokenEncrypted = "new-token", tokenType = "bearer")

        assertEquals("new-token", connection.accessTokenEncrypted)
        assertEquals("bearer", connection.tokenType)
        assertNull(connection.revokedAt)
    }

    @Test
    fun `updateWorkspace — 워크스페이스 표시 정보와 bot id 갱신`() {
        val connection = NotionConnection.connect(
            userId = 1L,
            notionWorkspaceId = "workspace-1",
            notionWorkspaceName = "Old Workspace",
            notionBotId = "old-bot",
            accessTokenEncrypted = "encrypted-token",
            tokenType = "bearer",
        )

        connection.updateWorkspace(notionWorkspaceName = "New Workspace", notionBotId = "new-bot")

        assertEquals("New Workspace", connection.notionWorkspaceName)
        assertEquals("new-bot", connection.notionBotId)
    }

    @Test
    fun `revoke — 연결 해제 시각 기록`() {
        val revokedAt = OffsetDateTime.parse("2026-05-20T10:00:00+09:00")
        val connection = NotionConnection.connect(
            userId = 1L,
            notionWorkspaceId = "workspace-1",
            notionWorkspaceName = "Workspace",
            notionBotId = "bot-1",
            accessTokenEncrypted = "encrypted-token",
            tokenType = "bearer",
        )

        connection.revoke(revokedAt)

        assertEquals(revokedAt, connection.revokedAt)
    }

    @Test
    fun `connect — tokenType은 비어 있을 수 없음`() {
        assertThrows(IllegalArgumentException::class.java) {
            NotionConnection.connect(
                userId = 1L,
                notionWorkspaceId = "workspace-1",
                notionWorkspaceName = "Workspace",
                notionBotId = "bot-1",
                accessTokenEncrypted = "encrypted-token",
                tokenType = "",
            )
        }
    }
}
