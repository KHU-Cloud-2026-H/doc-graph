package com.docgraph.backend.validation.query.interfaces.api

import com.docgraph.backend.auth.query.application.GetCurrentUserIdQuery
import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.project.query.application.SearchAdminProjectIdsByUserIdQuery
import com.docgraph.backend.validation.command.domain.ProjectValidationSetting
import com.docgraph.backend.validation.command.domain.ProjectValidationSettingRepository
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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.OffsetDateTime

class FakeGetCurrentUserIdQuery : GetCurrentUserIdQuery {
    @Volatile var userId: Long = 1L
    override fun get(): Long = userId
}

class FakeSearchAdminProjectIdsByUserIdQuery : SearchAdminProjectIdsByUserIdQuery {
    @Volatile var behavior: (Long) -> List<Long> = { emptyList() }
    override fun search(userId: Long): List<Long> = behavior(userId)
}

@TestConfiguration
class ProjectValidationQueryControllerTestConfig {
    @Bean @Primary fun fakeGetCurrentUserId() = FakeGetCurrentUserIdQuery()
    @Bean @Primary fun fakeSearchAdminProjectIds() = FakeSearchAdminProjectIdsByUserIdQuery()
}

@Tag("component")
@SpringBootTest
@AutoConfigureMockMvc
@Import(ProjectValidationQueryControllerTestConfig::class, SharedPostgresContainer::class)
class ProjectValidationQueryControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val settingRepository: ProjectValidationSettingRepository,
    private val getCurrentUserId: FakeGetCurrentUserIdQuery,
    private val searchAdminProjectIds: FakeSearchAdminProjectIdsByUserIdQuery,
) {

    @BeforeEach
    fun reset() {
        getCurrentUserId.userId = 1L
        searchAdminProjectIds.behavior = { emptyList() }
    }

    @Test
    fun `GET validation — Admin + 설정됨, 200 + enabled false`() {
        val projectId = uniqueProjectId()
        searchAdminProjectIds.behavior = { listOf(projectId) }
        settingRepository.save(
            ProjectValidationSetting(projectId = projectId, enabled = false, createdAt = OffsetDateTime.now()),
        )

        mockMvc.get("/projects/$projectId/validation").andExpect {
            status { isOk() }
            jsonPath("$.enabled") { value(false) }
        }
    }

    @Test
    fun `GET validation — Admin + 미설정, 200 + 기본 enabled true`() {
        val projectId = uniqueProjectId()
        searchAdminProjectIds.behavior = { listOf(projectId) }

        mockMvc.get("/projects/$projectId/validation").andExpect {
            status { isOk() }
            jsonPath("$.enabled") { value(true) }
        }
    }

    @Test
    fun `GET validation — 비Admin, 404`() {
        val projectId = uniqueProjectId()
        searchAdminProjectIds.behavior = { emptyList() }

        mockMvc.get("/projects/$projectId/validation").andExpect {
            status { isNotFound() }
        }
    }

    private fun uniqueProjectId(): Long = System.nanoTime()
}
