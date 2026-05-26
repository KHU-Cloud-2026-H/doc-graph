package com.docgraph.backend.acceptance

import com.docgraph.backend.auth.command.application.AuthSessionProperties
import com.docgraph.backend.auth.command.domain.SessionTokenService
import com.docgraph.backend.auth.query.infra.AuthQueryRepository
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Tag("unit")
class AcceptanceGetCurrentUserIdQueryHandlerTest {

    private val request = mockk<HttpServletRequest>()
    private val sessionProperties = AuthSessionProperties()
    private val sessionTokenService = SessionTokenService()
    private val authQueryRepository = mockk<AuthQueryRepository>()
    private val handler = AcceptanceGetCurrentUserIdQueryHandler(
        request, sessionProperties, sessionTokenService, authQueryRepository,
    )

    @Test
    fun `세션 쿠키의 token hash로 활성 세션 user id를 반환`() {
        val rawToken = "raw-session-token"
        every { request.cookies } returns arrayOf(Cookie(sessionProperties.cookieName, rawToken))
        every {
            authQueryRepository.findActiveSessionUserIdByTokenHash(eq(sessionTokenService.hash(rawToken)), any())
        } returns 42L

        assertEquals(42L, handler.get())
    }

    @Test
    fun `세션 쿠키 부재 시 IllegalStateException`() {
        every { request.cookies } returns null

        val ex = assertThrows<IllegalStateException> { handler.get() }
        assertTrue(ex.message!!.contains(sessionProperties.cookieName))
    }

    @Test
    fun `쿠키는 있으나 활성 세션이 없으면 IllegalStateException`() {
        val rawToken = "stale-token"
        every { request.cookies } returns arrayOf(Cookie(sessionProperties.cookieName, rawToken))
        every {
            authQueryRepository.findActiveSessionUserIdByTokenHash(eq(sessionTokenService.hash(rawToken)), any())
        } returns null

        assertThrows<IllegalStateException> { handler.get() }
    }
}