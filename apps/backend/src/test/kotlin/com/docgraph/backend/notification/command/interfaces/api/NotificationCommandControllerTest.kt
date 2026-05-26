package com.docgraph.backend.notification.command.interfaces.api

import com.docgraph.backend.auth.query.application.GetCurrentUserIdQuery
import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.notification.command.domain.ProjectWebhookRepository
import com.docgraph.backend.project.query.application.SearchAdminProjectIdsByUserIdQuery
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

class FakeGetCurrentUserIdQuery : GetCurrentUserIdQuery {
    @Volatile var userId: Long = 1L
    override fun get(): Long = userId
}

class FakeSearchAdminProjectIdsByUserIdQuery : SearchAdminProjectIdsByUserIdQuery {
    @Volatile var behavior: (Long) -> List<Long> = { emptyList() }
    override fun search(userId: Long): List<Long> = behavior(userId)
}

@TestConfiguration
class NotificationCommandControllerTestConfig {
    @Bean @Primary fun fakeGetCurrentUserId() = FakeGetCurrentUserIdQuery()
    @Bean @Primary fun fakeSearchAdminProjectIds() = FakeSearchAdminProjectIdsByUserIdQuery()
}

@Tag("component")
@SpringBootTest
@AutoConfigureMockMvc
@Import(NotificationCommandControllerTestConfig::class, SharedPostgresContainer::class)
class NotificationCommandControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val projectWebhookRepository: ProjectWebhookRepository,
    private val getCurrentUserId: FakeGetCurrentUserIdQuery,
    private val searchAdminProjectIds: FakeSearchAdminProjectIdsByUserIdQuery,
) {

    @BeforeEach
    fun reset() {
        getCurrentUserId.userId = 1L
        searchAdminProjectIds.behavior = { emptyList() }
    }

    @Test
    fun `PUT webhook — Admin, 204 + row 저장`() {
        val projectId = uniqueProjectId()
        searchAdminProjectIds.behavior = { if (it == 1L) listOf(projectId) else emptyList() }

        mockMvc.put("/projects/$projectId/webhook") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateWebhookRequest("https://hooks.slack.com/x"))
        }.andExpect {
            status { isNoContent() }
        }

        val saved = projectWebhookRepository.findByProjectId(projectId)
        assertNotNull(saved)
        assertEquals("https://hooks.slack.com/x", saved.url)
    }

    @Test
    fun `PUT webhook — 재호출 시 upsert, url 갱신`() {
        val projectId = uniqueProjectId()
        searchAdminProjectIds.behavior = { listOf(projectId) }

        putWebhook(projectId, "https://old")
        putWebhook(projectId, "https://new")

        val saved = projectWebhookRepository.findByProjectId(projectId)
        assertNotNull(saved)
        assertEquals("https://new", saved.url)
    }

    @Test
    fun `PUT webhook — 비Admin, 403`() {
        val projectId = uniqueProjectId()
        searchAdminProjectIds.behavior = { emptyList() }

        mockMvc.put("/projects/$projectId/webhook") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateWebhookRequest("https://x"))
        }.andExpect {
            status { isForbidden() }
        }
    }

    private fun putWebhook(projectId: Long, url: String) {
        mockMvc.put("/projects/$projectId/webhook") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateWebhookRequest(url))
        }.andExpect {
            status { isNoContent() }
        }
    }

    private fun uniqueProjectId(): Long = System.nanoTime()
}
