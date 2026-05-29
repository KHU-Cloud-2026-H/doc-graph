package com.docgraph.backend.acceptance

import com.docgraph.backend.auth.command.domain.SessionTokenService
import com.docgraph.backend.auth.command.domain.UserAccount
import com.docgraph.backend.auth.command.domain.UserAccountRepository
import com.docgraph.backend.auth.command.domain.UserSession
import com.docgraph.backend.auth.command.domain.UserSessionRepository
import com.docgraph.backend.auth.query.application.GetCurrentUserIdQuery
import com.docgraph.backend.fixtures.SharedPostgresContainer
import jakarta.servlet.ServletException
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import kotlin.test.assertTrue

@Tag("component")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("acceptance")
@Import(
    SharedPostgresContainer::class,
    AcceptanceGetCurrentUserIdQueryActiveProfileTest.TestEndpointConfig::class,
)
class AcceptanceGetCurrentUserIdQueryActiveProfileTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val query: GetCurrentUserIdQuery,
    private val userAccountRepository: UserAccountRepository,
    private val userSessionRepository: UserSessionRepository,
    private val sessionTokenService: SessionTokenService,
) {

    @TestConfiguration
    class TestEndpointConfig {
        @Bean
        fun currentMemberProbeController(query: GetCurrentUserIdQuery) = CurrentMemberProbeController(query)
    }

    @RestController
    class CurrentMemberProbeController(private val query: GetCurrentUserIdQuery) {
        @GetMapping("/test/internal/current-member")
        fun current(): Map<String, Long> = mapOf("memberId" to query.get())
    }

    @Test
    fun `acceptance profile에서 GetCurrentUserIdQuery 빈은 AcceptanceGetCurrentUserIdQueryHandler`() {
        assertTrue(query is AcceptanceGetCurrentUserIdQueryHandler)
    }

    @Test
    fun `세션 쿠키의 활성 세션 user id를 응답`() {
        val now = OffsetDateTime.now()
        val unique = System.nanoTime()
        val user = userAccountRepository.save(
            UserAccount.registerFromNotion(
                notionUserId = "notion-$unique",
                email = "probe-$unique@example.com",
                name = "Probe User",
                avatarUrl = null,
                now = now,
            ),
        )
        val rawToken = "probe-token-$unique"
        userSessionRepository.save(
            UserSession(
                userId = user.id,
                tokenHash = sessionTokenService.hash(rawToken),
                expiresAt = now.plusHours(1),
                createdAt = now,
            ),
        )

        mockMvc.get("/test/internal/current-member") {
            cookie(Cookie("DG_SESSION", rawToken))
        }.andExpect {
            status { isOk() }
            jsonPath("$.memberId") { value(user.id) }
        }
    }

    @Test
    fun `세션 쿠키 부재 시 핸들러가 IllegalStateException — MockMvc는 ServletException으로 전파`() {
        val ex = assertThrows<ServletException> {
            mockMvc.get("/test/internal/current-member")
        }
        assertTrue(ex.cause is IllegalStateException, "cause: ${ex.cause}")
    }
}