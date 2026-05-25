package com.docgraph.backend.acceptance

import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Tag("unit")
class AcceptanceGetCurrentUserIdQueryHandlerTest {

    private val request = mockk<HttpServletRequest>()
    private val handler = AcceptanceGetCurrentUserIdQueryHandler(request)

    @Test
    fun `헤더 값이 Long이면 그 값을 반환`() {
        every { request.getHeader("X-Test-User-Id") } returns "42"

        assertEquals(42L, handler.get())
    }

    @Test
    fun `헤더 부재 시 IllegalStateException`() {
        every { request.getHeader("X-Test-User-Id") } returns null

        val ex = assertThrows<IllegalStateException> { handler.get() }
        assertTrue(ex.message!!.contains("X-Test-User-Id"))
    }

    @Test
    fun `헤더 값이 Long 아니면 IllegalStateException`() {
        every { request.getHeader("X-Test-User-Id") } returns "not-a-number"

        val ex = assertThrows<IllegalStateException> { handler.get() }
        assertTrue(ex.message!!.contains("not-a-number"))
    }
}