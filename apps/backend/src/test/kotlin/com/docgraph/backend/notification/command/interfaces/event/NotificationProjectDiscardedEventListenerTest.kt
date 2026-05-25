package com.docgraph.backend.notification.command.interfaces.event

import com.docgraph.backend.notification.command.application.DiscardProjectWebhookCommand
import com.docgraph.backend.notification.command.application.DiscardProjectWebhookCommandHandler
import com.docgraph.backend.project.command.domain.ProjectDiscardedEvent
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

@Tag("unit")
class NotificationProjectDiscardedEventListenerTest {

    private val handler = mockk<DiscardProjectWebhookCommandHandler>()
    private val listener = NotificationProjectDiscardedEventListener(handler)

    @Test
    fun `프로젝트 삭제 이벤트 — webhook 정리 Command 위임`() {
        every { handler.handle(DiscardProjectWebhookCommand(projectId = 5L)) } just Runs

        listener.on(ProjectDiscardedEvent(projectId = 5L, occurredAt = OffsetDateTime.now()))

        verify(exactly = 1) { handler.handle(DiscardProjectWebhookCommand(projectId = 5L)) }
    }
}
