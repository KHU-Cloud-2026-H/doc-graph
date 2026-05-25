package com.docgraph.backend.notification.query.application

import com.docgraph.backend.notification.command.domain.ProjectWebhook
import com.docgraph.backend.notification.command.domain.ProjectWebhookRepository
import com.docgraph.backend.project.query.application.SearchAdminProjectIdsByUserIdQuery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Tag("unit")
class FindProjectWebhookQueryHandlerTest {

    private val repository = mockk<ProjectWebhookRepository>()
    private val searchAdminProjectIds = mockk<SearchAdminProjectIdsByUserIdQuery>()
    private val handler = FindProjectWebhookQueryHandler(repository, searchAdminProjectIds)

    @Test
    fun `Admin 프로젝트 + 설정됨 — url 반환`() {
        every { searchAdminProjectIds.search(7L) } returns listOf(10L)
        every { repository.findByProjectId(10L) } returns
            ProjectWebhook(id = 1L, projectId = 10L, url = "https://hooks.slack.com/x", createdAt = OffsetDateTime.now())

        val response = handler.find(projectId = 10L, userId = 7L)

        assertNotNull(response)
        assertEquals("https://hooks.slack.com/x", response.url)
    }

    @Test
    fun `Admin 프로젝트 + 미설정 — WebhookResponse url null`() {
        every { searchAdminProjectIds.search(7L) } returns listOf(10L)
        every { repository.findByProjectId(10L) } returns null

        val response = handler.find(projectId = 10L, userId = 7L)

        assertNotNull(response)
        assertNull(response.url)
    }

    @Test
    fun `비Admin 프로젝트 — null (controller 404)`() {
        every { searchAdminProjectIds.search(7L) } returns listOf(99L)

        assertNull(handler.find(projectId = 10L, userId = 7L))
    }
}
