package com.docgraph.backend.auth.command.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

@Tag("unit")
class UserAccountTest {

    @Test
    fun `registerFromNotion — Notion 사용자 정보로 계정 생성`() {
        val now = OffsetDateTime.parse("2026-05-20T10:00:00+09:00")

        val user = UserAccount.registerFromNotion(
            notionUserId = "notion-user-1",
            email = "user@example.com",
            name = "User",
            avatarUrl = "https://example.com/avatar.png",
            now = now,
        )

        assertEquals("notion-user-1", user.notionUserId)
        assertEquals("user@example.com", user.email)
        assertEquals("User", user.name)
        assertEquals("https://example.com/avatar.png", user.avatarUrl)
        assertEquals(now, user.createdAt)
        assertEquals(now, user.lastLoginAt)
    }

    @Test
    fun `updateProfile — OAuth 재로그인 시 표시 정보를 갱신`() {
        val user = UserAccount.registerFromNotion(
            notionUserId = "notion-user-1",
            email = "old@example.com",
            name = "Old Name",
            avatarUrl = null,
        )

        user.updateProfile(
            email = "new@example.com",
            name = "New Name",
            avatarUrl = "https://example.com/new-avatar.png",
        )

        assertEquals("new@example.com", user.email)
        assertEquals("New Name", user.name)
        assertEquals("https://example.com/new-avatar.png", user.avatarUrl)
    }

    @Test
    fun `markLoggedIn — 마지막 로그인 시각 갱신`() {
        val createdAt = OffsetDateTime.parse("2026-05-20T10:00:00+09:00")
        val loggedInAt = OffsetDateTime.parse("2026-05-20T11:00:00+09:00")
        val user = UserAccount.registerFromNotion(
            notionUserId = "notion-user-1",
            email = "user@example.com",
            name = "User",
            avatarUrl = null,
            now = createdAt,
        )

        user.markLoggedIn(loggedInAt)

        assertEquals(loggedInAt, user.lastLoginAt)
    }
}
