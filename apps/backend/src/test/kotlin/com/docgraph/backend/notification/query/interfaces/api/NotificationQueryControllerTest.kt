package com.docgraph.backend.notification.query.interfaces.api

import com.docgraph.backend.auth.query.application.GetCurrentUserIdQuery
import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.notification.command.domain.ProjectWebhook
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
class NotificationQueryControllerTestConfig {
    @Bean @Primary fun fakeGetCurrentUserId() = FakeGetCurrentUserIdQuery()
    @Bean @Primary fun fakeSearchAdminProjectIds() = FakeSearchAdminProjectIdsByUserIdQuery()
}

@Tag("component")
@SpringBootTest
@AutoConfigureMockMvc
@Import(NotificationQueryControllerTestConfig::class, SharedPostgresContainer::class)
class NotificationQueryControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
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
    fun `GET webhook — Admin + 설정됨, 200 + url`() {
        val projectId = uniqueProjectId()
        searchAdminProjectIds.behavior = { listOf(projectId) }
        projectWebhookRepository.save(
            ProjectWebhook(projectId = projectId, url = "https://hooks.slack.com/x", createdAt = OffsetDateTime.now()),
        )

        mockMvc.get("/projects/$projectId/webhook").andExpect {
            status { isOk() }
            jsonPath("$.url") { value("https://hooks.slack.com/x") }
        }
    }

    @Test
    fun `GET webhook — Admin + 미설정, 200 + url null`() {
        val projectId = uniqueProjectId()
        searchAdminProjectIds.behavior = { listOf(projectId) }

        mockMvc.get("/projects/$projectId/webhook").andExpect {
            status { isOk() }
            jsonPath("$.url") { value(null) }
        }
    }

    @Test
    fun `GET webhook — 비Admin, 404`() {
        val projectId = uniqueProjectId()
        searchAdminProjectIds.behavior = { emptyList() }

        mockMvc.get("/projects/$projectId/webhook").andExpect {
            status { isNotFound() }
        }
    }

    private fun uniqueProjectId(): Long = System.nanoTime()
}
