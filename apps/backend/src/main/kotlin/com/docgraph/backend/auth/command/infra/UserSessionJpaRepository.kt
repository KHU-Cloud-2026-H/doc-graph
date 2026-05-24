package com.docgraph.backend.auth.command.infra

import com.docgraph.backend.auth.command.domain.UserSession
import org.springframework.data.jpa.repository.JpaRepository

interface UserSessionJpaRepository : JpaRepository<UserSession, Long> {
    fun findByTokenHash(tokenHash: String): UserSession?
}
