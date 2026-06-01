package com.docgraph.backend.validation.command.interfaces.event

import com.docgraph.backend.project.command.domain.ProjectDiscardedEvent
import com.docgraph.backend.validation.command.application.DiscardProjectValidationCommand
import com.docgraph.backend.validation.command.application.DiscardProjectValidationCommandHandler
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

@Tag("unit")
class ValidationProjectDiscardedEventListenerTest {

    private val handler = mockk<DiscardProjectValidationCommandHandler>(relaxUnitFun = true)
    private val listener = ValidationProjectDiscardedEventListener(handler)

    @Test
    fun `프로젝트 삭제 이벤트 — setting 정리 Command 위임`() {
        listener.on(ProjectDiscardedEvent(projectId = 5L, occurredAt = OffsetDateTime.now()))

        verify(exactly = 1) { handler.handle(DiscardProjectValidationCommand(projectId = 5L)) }
    }
}
