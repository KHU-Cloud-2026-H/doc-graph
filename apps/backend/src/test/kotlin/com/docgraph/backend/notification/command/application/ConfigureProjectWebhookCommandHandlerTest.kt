package com.docgraph.backend.notification.command.application

import com.docgraph.backend.notification.command.domain.NotificationPermissionDeniedException
import com.docgraph.backend.notification.command.domain.ProjectWebhook
import com.docgraph.backend.notification.command.domain.ProjectWebhookRepository
import com.docgraph.backend.project.query.application.SearchAdminProjectIdsByUserIdQuery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.OffsetDateTime
import kotlin.test.assertEquals

@Tag("unit")
class ConfigureProjectWebhookCommandHandlerTest {

    private val repository = mockk<ProjectWebhookRepository>()
    private val searchAdminProjectIds = mockk<SearchAdminProjectIdsByUserIdQuery>()
    private val handler = ConfigureProjectWebhookCommandHandler(repository, searchAdminProjectIds)

    @Test
    fun `Admin 프로젝트 + 미설정 — 새 ProjectWebhook save`() {
        every { searchAdminProjectIds.search(7L) } returns listOf(10L, 20L)
        every { repository.findByProjectId(10L) } returns null
        val captured = slot<ProjectWebhook>()
        every { repository.save(capture(captured)) } answers { captured.captured.also { it.id = 1L } }

        handler.handle(ConfigureProjectWebhookCommand(projectId = 10L, requesterUserId = 7L, url = "https://hooks.slack.com/x"))

        assertEquals(10L, captured.captured.projectId)
        assertEquals("https://hooks.slack.com/x", captured.captured.url)
        verify(exactly = 1) { repository.save(any()) }
    }

    @Test
    fun `Admin 프로젝트 + 기존 — 기존 row url 갱신, 신규 insert 없음`() {
        val existing = ProjectWebhook(id = 5L, projectId = 10L, url = "https://old", createdAt = OffsetDateTime.now())
        every { searchAdminProjectIds.search(7L) } returns listOf(10L)
        every { repository.findByProjectId(10L) } returns existing
        every { repository.save(existing) } returns existing

        handler.handle(ConfigureProjectWebhookCommand(projectId = 10L, requesterUserId = 7L, url = "https://new"))

        assertEquals("https://new", existing.url)
    }

    @Test
    fun `비Admin 프로젝트 — NotificationPermissionDeniedException, save 호출 없음`() {
        every { searchAdminProjectIds.search(7L) } returns listOf(99L)

        assertThrows<NotificationPermissionDeniedException> {
            handler.handle(ConfigureProjectWebhookCommand(projectId = 10L, requesterUserId = 7L, url = "https://x"))
        }
        verify(exactly = 0) { repository.save(any()) }
    }
}
