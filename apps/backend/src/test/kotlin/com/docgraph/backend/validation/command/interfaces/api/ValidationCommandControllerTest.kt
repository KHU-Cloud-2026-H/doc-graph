package com.docgraph.backend.validation.command.interfaces.api

import com.docgraph.backend.auth.query.application.GetCurrentMemberQuery
import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.validation.command.domain.Conflict
import com.docgraph.backend.validation.command.domain.ConflictRepository
import tools.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.post
import java.time.OffsetDateTime

class FakeGetCurrentMemberQuery : GetCurrentMemberQuery {
    @Volatile var memberId: Long = 7L
    override fun get(): Long = memberId
}

@TestConfiguration
class ValidationCommandControllerTestConfig {
    @Bean @Primary fun fakeGetCurrentMember() = FakeGetCurrentMemberQuery()
}

@Tag("component")
@SpringBootTest
@AutoConfigureMockMvc
@Import(ValidationCommandControllerTestConfig::class, SharedPostgresContainer::class)
class ValidationCommandControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val conflictRepository: ConflictRepository,
    private val getCurrentMember: FakeGetCurrentMemberQuery,
) {

    @BeforeEach
    fun reset() {
        conflictRepository.deleteAll()
        getCurrentMember.memberId = 7L
    }

    @Test
    fun `POST ignore — 활성 충돌 → 204, DB에 ignored 상태`() {
        val conflict = save(newActive(edgeId = 51L))

        mockMvc.post("/conflicts/${conflict.id}/ignore") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(IgnoreConflictRequest("의도된 차이"))
        }.andExpect {
            status { isNoContent() }
        }

        val reloaded = conflictRepository.findById(conflict.id).orElseThrow()
        assert(reloaded.ignoredAt != null)
        assert(reloaded.ignoredBy == 7L)
        assert(reloaded.ignoreReason == "의도된 차이")
    }

    @Test
    fun `POST ignore — 이미 무시 상태 → 409`() {
        val conflict = save(
            newActive(edgeId = 52L).apply {
                ignore(by = 1L, reason = "기존 무시", at = OffsetDateTime.now())
            },
        )

        mockMvc.post("/conflicts/${conflict.id}/ignore") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(IgnoreConflictRequest(null))
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `POST ignore — 이미 해소된 충돌 → 409`() {
        val conflict = save(
            newActive(edgeId = 53L).apply {
                markResolved(OffsetDateTime.now())
            },
        )

        mockMvc.post("/conflicts/${conflict.id}/ignore") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(IgnoreConflictRequest(null))
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `POST ignore — 존재 X → 404`() {
        mockMvc.post("/conflicts/9999/ignore") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(IgnoreConflictRequest(null))
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `DELETE ignore — 무시 상태 → 204, DB에서 ignored 해제`() {
        val conflict = save(
            newActive(edgeId = 54L).apply {
                ignore(by = 1L, reason = "테스트", at = OffsetDateTime.now())
            },
        )

        mockMvc.delete("/conflicts/${conflict.id}/ignore").andExpect {
            status { isNoContent() }
        }

        val reloaded = conflictRepository.findById(conflict.id).orElseThrow()
        assert(reloaded.ignoredAt == null)
        assert(reloaded.ignoredBy == null)
        assert(reloaded.ignoreReason == null)
    }

    @Test
    fun `DELETE ignore — 무시 상태 아님 → 409`() {
        val conflict = save(newActive(edgeId = 55L))

        mockMvc.delete("/conflicts/${conflict.id}/ignore").andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `DELETE ignore — 존재 X → 404`() {
        mockMvc.delete("/conflicts/9999/ignore").andExpect {
            status { isNotFound() }
        }
    }

    private fun newActive(edgeId: Long): Conflict {
        val now = OffsetDateTime.now()
        return Conflict(edgeId = edgeId, firstDetectedAt = now, lastDetectedAt = now)
    }

    private fun save(conflict: Conflict): Conflict = conflictRepository.saveAndFlush(conflict)
}
