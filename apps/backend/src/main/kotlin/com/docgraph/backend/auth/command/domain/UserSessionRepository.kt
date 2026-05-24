package com.docgraph.backend.auth.command.domain

interface UserSessionRepository {
    fun save(session: UserSession): UserSession
    fun findByTokenHash(tokenHash: String): UserSession?
    fun findById(id: Long): UserSession?
}
