package com.docgraph.backend.validation.command.interfaces.api

import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.project.query.application.SearchAdminProjectIdsByUserIdQuery
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
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.put
import tools.jackson.databind.ObjectMapper
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

// FakeGetCurrentUserIdQuery는 같은 패키지의 ValidationCommandControllerTest.kt에 이미 선언됨 — 재사용.
class FakeSearchAdminProjectIdsByUserIdQuery : SearchAdminProjectIdsByUserIdQuery {
    @Volatile var behavior: (Long) -> List<Long> = { emptyList() }
    override fun search(userId: Long): List<Long> = behavior(userId)
}

@TestConfiguration
class ProjectValidationCommandControllerTestConfig {
    @Bean @Primary fun fakeGetCurrentUserId() = FakeGetCurrentUserIdQuery()
    @Bean @Primary fun fakeSearchAdminProjectIds() = FakeSearchAdminProjectIdsByUserIdQuery()
}

@Tag("component")
@SpringBootTest
@AutoConfigureMockMvc
@Import(ProjectValidationCommandControllerTestConfig::class, SharedPostgresContainer::class)
class ProjectValidationCommandControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
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
    fun `PUT validation — Admin, 204 + enabled 저장`() {
        val projectId = uniqueProjectId()
        searchAdminProjectIds.behavior = { if (it == 1L) listOf(projectId) else emptyList() }

        mockMvc.put("/projects/$projectId/validation") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateProjectValidationRequest(enabled = false))
        }.andExpect {
            status { isNoContent() }
        }

        val saved = settingRepository.findByProjectId(projectId)
        assertNotNull(saved)
        assertEquals(false, saved.enabled)
    }

    @Test
    fun `PUT validation — 재호출 시 upsert, enabled 갱신`() {
        val projectId = uniqueProjectId()
        searchAdminProjectIds.behavior = { listOf(projectId) }

        putValidation(projectId, false)
        putValidation(projectId, true)

        val saved = settingRepository.findByProjectId(projectId)
        assertNotNull(saved)
        assertEquals(true, saved.enabled)
    }

    @Test
    fun `PUT validation — 비Admin, 403`() {
        val projectId = uniqueProjectId()
        searchAdminProjectIds.behavior = { emptyList() }

        mockMvc.put("/projects/$projectId/validation") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateProjectValidationRequest(enabled = false))
        }.andExpect {
            status { isForbidden() }
        }
    }

    private fun putValidation(projectId: Long, enabled: Boolean) {
        mockMvc.put("/projects/$projectId/validation") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateProjectValidationRequest(enabled = enabled))
        }.andExpect {
            status { isNoContent() }
        }
    }

    private fun uniqueProjectId(): Long = System.nanoTime()
}
