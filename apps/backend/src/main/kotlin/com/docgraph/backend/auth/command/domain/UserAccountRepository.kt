package com.docgraph.backend.auth.command.domain

interface UserAccountRepository {
    fun save(userAccount: UserAccount): UserAccount
    fun findById(id: Long): UserAccount?
    fun findByEmail(email: String): UserAccount?
    fun findByNotionUserId(notionUserId: String): UserAccount?
}
