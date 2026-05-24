package com.docgraph.backend.auth.command.infra

import com.docgraph.backend.auth.command.domain.UserSession
import com.docgraph.backend.auth.command.domain.UserSessionRepository
import org.springframework.stereotype.Component

@Component
class UserSessionRepositoryImpl(
    private val jpa: UserSessionJpaRepository,
) : UserSessionRepository {
    override fun save(session: UserSession): UserSession = jpa.save(session)
    override fun findByTokenHash(tokenHash: String): UserSession? = jpa.findByTokenHash(tokenHash)
    override fun findById(id: Long): UserSession? = jpa.findById(id).orElse(null)
}
