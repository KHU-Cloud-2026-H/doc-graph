package com.docgraph.backend.auth.command.application

import com.docgraph.backend.auth.command.domain.AccessTokenEncryptor
import com.docgraph.backend.auth.command.domain.NotionConnection
import com.docgraph.backend.auth.command.domain.NotionConnectionRepository
import com.docgraph.backend.auth.command.domain.NotionOAuthClient
import com.docgraph.backend.auth.command.domain.NotionOAuthToken
import com.docgraph.backend.auth.command.domain.NotionOAuthUser
import com.docgraph.backend.auth.command.domain.NotionWorkspaceAuthorizedEvent
import com.docgraph.backend.auth.command.domain.SessionTokenService
import com.docgraph.backend.auth.command.domain.UserAccount
import com.docgraph.backend.auth.command.domain.UserAccountRepository
import com.docgraph.backend.auth.command.domain.UserSession
import com.docgraph.backend.auth.command.domain.UserSessionRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class CompleteNotionOAuthCommandHandler(
    private val notionOAuthClient: NotionOAuthClient,
    private val accessTokenEncryptor: AccessTokenEncryptor,
    private val userAccountRepository: UserAccountRepository,
    private val notionConnectionRepository: NotionConnectionRepository,
    private val userSessionRepository: UserSessionRepository,
    private val sessionTokenService: SessionTokenService,
    private val sessionProperties: AuthSessionProperties,
    private val publisher: ApplicationEventPublisher,
) {
    @Transactional
    fun handle(command: CompleteNotionOAuthCommand): CompleteNotionOAuthResult {
        val now = OffsetDateTime.now()
        val token = notionOAuthClient.exchangeAuthorizationCode(command.code)
        val user = upsertUser(token.owner, now)
        upsertConnection(user.id, token, now)
        val sessionToken = issueSession(user.id, now)
        publisher.publishEvent(
            NotionWorkspaceAuthorizedEvent(
                ownerUserId = user.id,
                notionWorkspaceId = token.workspaceId,
                notionWorkspaceName = token.workspaceName,
                notionBotId = token.botId,
                occurredAt = now,
            ),
        )
        return CompleteNotionOAuthResult(
            userId = user.id,
            sessionToken = sessionToken,
            sessionExpiresInSeconds = sessionProperties.ttlSeconds,
        )
    }

    private fun upsertUser(owner: NotionOAuthUser, now: OffsetDateTime): UserAccount {
        val email = owner.email.lowercase()
        val user = userAccountRepository.findByNotionUserId(owner.notionUserId)
            ?: userAccountRepository.findByEmail(email)
            ?: UserAccount.registerFromNotion(
                notionUserId = owner.notionUserId,
                email = email,
                name = owner.name,
                avatarUrl = owner.avatarUrl,
                now = now,
            )
        user.updateProfile(email, owner.name, owner.avatarUrl)
        user.markLoggedIn(now)
        return userAccountRepository.save(user)
    }

    private fun upsertConnection(userId: Long, token: NotionOAuthToken, now: OffsetDateTime) {
        val encryptedAccessToken = accessTokenEncryptor.encrypt(token.accessToken)
        val existing = notionConnectionRepository.findByUserIdAndNotionWorkspaceId(userId, token.workspaceId)
        if (existing == null) {
            notionConnectionRepository.save(
                NotionConnection.connect(
                    userId = userId,
                    notionWorkspaceId = token.workspaceId,
                    notionWorkspaceName = token.workspaceName,
                    notionBotId = token.botId,
                    accessTokenEncrypted = encryptedAccessToken,
                    tokenType = token.tokenType,
                    now = now,
                ),
            )
            return
        }
        existing.reconnect(
            accessTokenEncrypted = encryptedAccessToken,
            tokenType = token.tokenType,
            notionWorkspaceName = token.workspaceName,
            notionBotId = token.botId,
        )
        notionConnectionRepository.save(existing)
    }

    private fun issueSession(userId: Long, now: OffsetDateTime): String {
        val rawToken = sessionTokenService.generate()
        userSessionRepository.save(
            UserSession(
                userId = userId,
                tokenHash = sessionTokenService.hash(rawToken),
                expiresAt = now.plusSeconds(sessionProperties.ttlSeconds),
                createdAt = now,
            ),
        )
        return rawToken
    }
}
