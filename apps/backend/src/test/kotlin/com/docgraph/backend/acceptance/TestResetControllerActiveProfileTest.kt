package com.docgraph.backend.acceptance

import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.validation.command.domain.Conflict
import com.docgraph.backend.validation.command.domain.ConflictRepository
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.OffsetDateTime

@Tag("component")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("acceptance")
@Import(SharedPostgresContainer::class)
class TestResetControllerActiveProfileTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val conflictRepository: ConflictRepository,
    private val jdbcTemplate: JdbcTemplate,
) {

    @Test
    fun `POST test reset — 204, 비-Flyway 테이블 비어짐, Flyway 메타테이블 보존`() {
        conflictRepository.saveAndFlush(
            Conflict(
                edgeId = 100L,
                firstDetectedAt = OffsetDateTime.now(),
                lastDetectedAt = OffsetDateTime.now(),
            ),
        )
        assert(conflictRepository.count() > 0)

        mockMvc.post("/test/reset").andExpect {
            status { isNoContent() }
        }

        assert(conflictRepository.count() == 0L)
        val flywayCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history",
            Long::class.java,
        )
        assert((flywayCount ?: 0L) > 0)
    }
}