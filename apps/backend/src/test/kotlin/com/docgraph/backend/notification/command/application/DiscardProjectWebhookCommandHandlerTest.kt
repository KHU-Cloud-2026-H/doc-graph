package com.docgraph.backend.notification.command.application

import com.docgraph.backend.notification.command.domain.ProjectWebhookRepository
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("unit")
class DiscardProjectWebhookCommandHandlerTest {

    private val repository = mockk<ProjectWebhookRepository>()
    private val handler = DiscardProjectWebhookCommandHandler(repository)

    @Test
    fun `프로젝트 webhook 삭제 위임`() {
        every { repository.deleteByProjectId(5L) } just Runs

        handler.handle(DiscardProjectWebhookCommand(projectId = 5L))

        verify(exactly = 1) { repository.deleteByProjectId(5L) }
    }
}
