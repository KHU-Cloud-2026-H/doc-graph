package com.docgraph.backend.acceptance

import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Tag("component")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("acceptance")
@Import(SharedPostgresContainer::class)
class TestLoginControllerActiveProfileTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {
    private val objectMapper = ObjectMapper()

    private fun login(key: String? = null): Cookie {
        val result = mockMvc.get("/test/login") {
            if (key != null) param("key", key)
        }.andExpect {
            status { isFound() }
        }.andReturn()
        return assertNotNull(result.response.getCookie("DG_SESSION"), "세션 쿠키 미발급")
    }

    private fun currentUserId(cookie: Cookie): Long {
        val body = mockMvc.get("/auth/me") { cookie(cookie) }
            .andExpect { status { isOk() } }
            .andReturn().response.contentAsString
        return objectMapper.readTree(body)["id"].asLong()
    }

    @Test
    fun `GET test login — 세션 쿠키 Set + workspaces redirect`() {
        val cookie = login()
        assertTrue(cookie.value.isNotBlank(), "세션 토큰이 비어있음")
    }

    @Test
    fun `발급된 세션 쿠키로 auth me가 user id를 반환`() {
        val userId = currentUserId(login(key = "owner"))
        assertTrue(userId > 0, "user id가 양수가 아님: $userId")
    }

    @Test
    fun `같은 key 재로그인은 동일 user`() {
        val first = currentUserId(login(key = "shared"))
        val second = currentUserId(login(key = "shared"))
        assertEquals(first, second)
    }

    @Test
    fun `다른 key는 다른 user`() {
        val a = currentUserId(login(key = "user-a"))
        val b = currentUserId(login(key = "user-b"))
        assertNotEquals(a, b)
    }
}