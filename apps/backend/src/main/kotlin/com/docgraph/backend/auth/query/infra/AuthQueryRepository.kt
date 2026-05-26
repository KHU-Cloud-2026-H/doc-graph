package com.docgraph.backend.auth.query.infra

import com.docgraph.backend.auth.command.domain.QUserAccount
import com.docgraph.backend.auth.command.domain.QUserSession
import com.docgraph.backend.auth.query.application.UserResponse
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class AuthQueryRepository {
    @PersistenceContext
    private lateinit var entityManager: EntityManager

    private val queryFactory: JPAQueryFactory
        get() = JPAQueryFactory(entityManager)

    fun findUserIdByEmail(email: String): Long? {
        val user = QUserAccount.userAccount
        return queryFactory
            .select(user.id)
            .from(user)
            .where(user.email.eq(email.lowercase()))
            .fetchFirst()
    }

    fun findActiveSessionUserIdByTokenHash(tokenHash: String, now: OffsetDateTime = OffsetDateTime.now()): Long? {
        val session = QUserSession.userSession
        return queryFactory
            .select(session.userId)
            .from(session)
            .where(
                session.tokenHash.eq(tokenHash),
                session.revokedAt.isNull,
                session.expiresAt.after(now),
            )
            .fetchFirst()
    }

    fun searchUserAccountsByIds(userIds: Collection<Long>): List<UserResponse> {
        if (userIds.isEmpty()) {
            return emptyList()
        }
        val user = QUserAccount.userAccount
        return queryFactory
            .select(
                Projections.constructor(
                    UserResponse::class.java,
                    user.id,
                    user.email,
                    user.name,
                    user.avatarUrl,
                ),
            )
            .from(user)
            .where(user.id.`in`(userIds))
            .fetch()
    }
}
