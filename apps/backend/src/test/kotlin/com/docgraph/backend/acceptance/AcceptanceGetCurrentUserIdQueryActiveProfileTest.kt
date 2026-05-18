package com.docgraph.backend.acceptance

import com.docgraph.backend.auth.query.application.GetCurrentUserIdQuery
import com.docgraph.backend.fixtures.SharedPostgresContainer
import jakarta.servlet.ServletException
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
    fun `X-Test-User-Id 헤더 값을 그대로 응답`() {
        mockMvc.get("/test/internal/current-member") {
            header("X-Test-User-Id", "777")
        }.andExpect {
            status { isOk() }
            jsonPath("$.memberId") { value(777) }
        }
    }

    @Test
    fun `헤더 부재 시 핸들러가 IllegalStateException — MockMvc는 ServletException으로 전파`() {
        val ex = assertThrows<ServletException> {
            mockMvc.get("/test/internal/current-member")
        }
        assertTrue(ex.cause is IllegalStateException, "cause: ${ex.cause}")
    }
}