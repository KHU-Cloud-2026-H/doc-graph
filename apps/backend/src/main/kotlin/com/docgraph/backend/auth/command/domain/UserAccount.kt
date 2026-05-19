package com.docgraph.backend.auth.command.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "user_account")
class UserAccount(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @Column(name = "email", nullable = false, length = 320)
    var email: String,

    @Column(name = "name", nullable = false, length = 200)
    var name: String,

    @Column(name = "avatar_url", length = 1000)
    var avatarUrl: String? = null,

    @Column(name = "notion_user_id", nullable = false, length = 100)
    val notionUserId: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "last_login_at", nullable = false)
    var lastLoginAt: OffsetDateTime = createdAt,
) {
    fun updateProfile(email: String, name: String, avatarUrl: String?) {
        this.email = email
        this.name = name
        this.avatarUrl = avatarUrl
    }

    fun markLoggedIn(at: OffsetDateTime = OffsetDateTime.now()) {
        lastLoginAt = at
    }

    companion object {
        fun registerFromNotion(
            notionUserId: String,
            email: String,
            name: String,
            avatarUrl: String?,
            now: OffsetDateTime = OffsetDateTime.now(),
        ): UserAccount = UserAccount(
            notionUserId = notionUserId,
            email = email,
            name = name,
            avatarUrl = avatarUrl,
            createdAt = now,
            lastLoginAt = now,
        )
    }
}
