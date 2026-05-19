package com.docgraph.backend.auth.command.domain

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

@Tag("unit")
class UserSessionTest {

    @Test
    fun `ensureActive — 만료 전이고 revoke되지 않은 세션은 통과`() {
        val now = OffsetDateTime.parse("2026-05-20T10:00:00+09:00")
        val session = UserSession(
            id = 1L,
            userId = 10L,
            tokenHash = "token-hash",
            expiresAt = now.plusDays(1),
            createdAt = now,
        )

        assertDoesNotThrow { session.ensureActive(now) }
    }

    @Test
    fun `ensureActive — 만료된 세션은 AuthSessionInactiveException`() {
        val now = OffsetDateTime.parse("2026-05-20T10:00:00+09:00")
        val session = UserSession(
            id = 1L,
            userId = 10L,
            tokenHash = "token-hash",
            expiresAt = now.minusSeconds(1),
            createdAt = now.minusDays(1),
        )

        assertThrows(AuthSessionInactiveException::class.java) {
            session.ensureActive(now)
        }
    }

    @Test
    fun `revoke — revoke 시각 기록 후 비활성 처리`() {
        val now = OffsetDateTime.parse("2026-05-20T10:00:00+09:00")
        val revokedAt = now.plusHours(1)
        val session = UserSession(
            id = 1L,
            userId = 10L,
            tokenHash = "token-hash",
            expiresAt = now.plusDays(1),
            createdAt = now,
        )

        session.revoke(revokedAt)

        assertEquals(revokedAt, session.revokedAt)
        assertThrows(AuthSessionInactiveException::class.java) {
            session.ensureActive(now)
        }
    }
}
