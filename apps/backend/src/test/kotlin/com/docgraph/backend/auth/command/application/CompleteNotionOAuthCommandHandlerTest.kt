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
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

@Tag("unit")
class CompleteNotionOAuthCommandHandlerTest {

    private val users = InMemoryUserAccountRepository()
    private val connections = InMemoryNotionConnectionRepository()
    private val sessions = InMemoryUserSessionRepository()
    private val events = mutableListOf<Any>()
    private val handler = CompleteNotionOAuthCommandHandler(
        notionOAuthClient = FakeNotionOAuthClient(),
        accessTokenEncryptor = AccessTokenEncryptor { "encrypted-$it" },
        userAccountRepository = users,
        notionConnectionRepository = connections,
        userSessionRepository = sessions,
        sessionTokenService = SessionTokenService(),
        sessionProperties = AuthSessionProperties(ttlSeconds = 3600),
        publisher = ApplicationEventPublisher { events.add(it) },
    )

    @Test
    fun `Notion OAuth callback completion upserts user connection session and publishes workspace event`() {
        val result = handler.handle(CompleteNotionOAuthCommand(code = "oauth-code"))

        assertEquals(1L, result.userId)
        assertEquals(3600, result.sessionExpiresInSeconds)
        assertNotNull(result.sessionToken)

        val user = users.findById(result.userId)
        assertNotNull(user)
        assertEquals("user@example.com", user.email)
        assertEquals("Notion User", user.name)

        val connection = connections.findByUserIdAndNotionWorkspaceId(result.userId, "workspace-1")
        assertNotNull(connection)
        assertEquals("Workspace", connection.notionWorkspaceName)
        assertEquals("bot-1", connection.notionBotId)
        assertEquals("encrypted-ntn-token", connection.accessTokenEncrypted)

        assertEquals(1, sessions.saved.size)
        assertEquals(result.userId, sessions.saved.single().userId)

        val event = assertIs<NotionWorkspaceAuthorizedEvent>(events.single())
        assertEquals(result.userId, event.ownerUserId)
        assertEquals("workspace-1", event.notionWorkspaceId)
        assertEquals("Workspace", event.notionWorkspaceName)
        assertEquals("bot-1", event.notionBotId)
    }

    @Test
    fun `existing Notion user and connection are updated on reconnect`() {
        handler.handle(CompleteNotionOAuthCommand(code = "first"))
        val result = handler.handle(CompleteNotionOAuthCommand(code = "second"))

        assertEquals(1L, result.userId)
        assertEquals(1, users.saved.size)
        assertEquals(1, connections.saved.size)
        assertEquals(2, sessions.saved.size)
    }

    private class FakeNotionOAuthClient : NotionOAuthClient {
        override fun exchangeAuthorizationCode(code: String): NotionOAuthToken =
            NotionOAuthToken(
                accessToken = "ntn-token",
                tokenType = "bearer",
                botId = "bot-1",
                workspaceId = "workspace-1",
                workspaceName = "Workspace",
                owner = NotionOAuthUser(
                    notionUserId = "notion-user-1",
                    email = "USER@example.com",
                    name = "Notion User",
                    avatarUrl = null,
                ),
            )
    }

    private class InMemoryUserAccountRepository : UserAccountRepository {
        val saved = mutableMapOf<Long, UserAccount>()
        private var nextId = 1L

        override fun save(userAccount: UserAccount): UserAccount {
            if (userAccount.id == 0L) {
                userAccount.id = nextId++
            }
            saved[userAccount.id] = userAccount
            return userAccount
        }

        override fun findById(id: Long): UserAccount? = saved[id]
        override fun findByEmail(email: String): UserAccount? =
            saved.values.firstOrNull { it.email == email }

        override fun findByNotionUserId(notionUserId: String): UserAccount? =
            saved.values.firstOrNull { it.notionUserId == notionUserId }

        override fun findAllByIdIn(ids: Collection<Long>): List<UserAccount> =
            saved.values.filter { it.id in ids }
    }

    private class InMemoryNotionConnectionRepository : NotionConnectionRepository {
        val saved = mutableMapOf<Pair<Long, String>, NotionConnection>()
        private var nextId = 1L

        override fun save(connection: NotionConnection): NotionConnection {
            if (connection.id == 0L) {
                connection.id = nextId++
            }
            saved[connection.userId to connection.notionWorkspaceId] = connection
            return connection
        }

        override fun findByUserIdAndNotionWorkspaceId(userId: Long, notionWorkspaceId: String): NotionConnection? =
            saved[userId to notionWorkspaceId]

        override fun findAllByNotionWorkspaceId(notionWorkspaceId: String): List<NotionConnection> =
            saved.values.filter { it.notionWorkspaceId == notionWorkspaceId }
    }

    private class InMemoryUserSessionRepository : UserSessionRepository {
        val saved = mutableListOf<UserSession>()

        override fun save(session: UserSession): UserSession {
            session.id = (saved.size + 1).toLong()
            saved.add(session)
            return session
        }

        override fun findByTokenHash(tokenHash: String): UserSession? =
            saved.firstOrNull { it.tokenHash == tokenHash }

        override fun findById(id: Long): UserSession? =
            saved.firstOrNull { it.id == id }
    }
}
