package com.docgraph.backend.auth.query.application

import com.docgraph.backend.auth.command.domain.SessionTokenService
import com.docgraph.backend.auth.query.infra.AuthQueryRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

@Tag("unit")
class GetCurrentUserIdQueryHandlerTest {

    private val tokenService = SessionTokenService()
    private val rawToken = "raw-session"
    private val repository = mockk<AuthQueryRepository>()

    @Test
    fun `valid session token returns user id`() {
        every { repository.findActiveSessionUserIdByTokenHash(tokenService.hash(rawToken), any()) } returns 99L
        val handler = GetCurrentUserIdQueryHandler(
            currentSessionTokenProvider = CurrentSessionTokenProvider { rawToken },
            sessionTokenService = tokenService,
            repository = repository,
        )

        assertEquals(99L, handler.get())
    }

    @Test
    fun `missing session token fails`() {
        val handler = GetCurrentUserIdQueryHandler(
            currentSessionTokenProvider = CurrentSessionTokenProvider { null },
            sessionTokenService = tokenService,
            repository = repository,
        )

        assertThrows<IllegalStateException> { handler.get() }
    }
}
