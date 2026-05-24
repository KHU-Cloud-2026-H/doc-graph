package com.docgraph.backend.auth.command.infra

import com.docgraph.backend.auth.command.domain.UserAccount
import com.docgraph.backend.auth.command.domain.UserAccountRepository
import org.springframework.stereotype.Component

@Component
class UserAccountRepositoryImpl(
    private val jpa: UserAccountJpaRepository,
) : UserAccountRepository {
    override fun save(userAccount: UserAccount): UserAccount = jpa.save(userAccount)
    override fun findById(id: Long): UserAccount? = jpa.findById(id).orElse(null)
    override fun findByEmail(email: String): UserAccount? = jpa.findByEmail(email)
    override fun findByNotionUserId(notionUserId: String): UserAccount? = jpa.findByNotionUserId(notionUserId)
    override fun findAllByIdIn(ids: Collection<Long>): List<UserAccount> = jpa.findAllByIdIn(ids)
}
