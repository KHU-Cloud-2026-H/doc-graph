package com.docgraph.backend.auth.command.infra

import com.docgraph.backend.auth.command.domain.UserAccount
import org.springframework.data.jpa.repository.JpaRepository

interface UserAccountJpaRepository : JpaRepository<UserAccount, Long> {
    fun findByEmail(email: String): UserAccount?
    fun findByNotionUserId(notionUserId: String): UserAccount?
    fun findAllByIdIn(ids: Collection<Long>): List<UserAccount>
}
