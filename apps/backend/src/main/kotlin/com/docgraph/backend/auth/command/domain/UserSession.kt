package com.docgraph.backend.auth.command.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "user_session")
class UserSession(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "token_hash", nullable = false, length = 200)
    val tokenHash: String,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: OffsetDateTime,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "revoked_at")
    var revokedAt: OffsetDateTime? = null,
) {
    fun revoke(at: OffsetDateTime = OffsetDateTime.now()) {
        revokedAt = at
    }

    fun ensureActive(now: OffsetDateTime = OffsetDateTime.now()) {
        if (revokedAt != null || !expiresAt.isAfter(now)) {
            throw AuthSessionInactiveException(id)
        }
    }
}
