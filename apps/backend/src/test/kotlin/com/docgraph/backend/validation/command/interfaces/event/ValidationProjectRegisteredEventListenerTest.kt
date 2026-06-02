package com.docgraph.backend.validation.command.interfaces.event

import com.docgraph.backend.project.command.domain.ProjectRegisteredEvent
import com.docgraph.backend.validation.command.application.InitializeProjectValidationCommand
import com.docgraph.backend.validation.command.application.InitializeProjectValidationCommandHandler
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

@Tag("unit")
class ValidationProjectRegisteredEventListenerTest {

    private val handler = mockk<InitializeProjectValidationCommandHandler>(relaxUnitFun = true)
    private val listener = ValidationProjectRegisteredEventListener(handler)

    @Test
    fun `validationEnabled=false — disabled setting init 호출`() {
        listener.on(ProjectRegisteredEvent(projectId = 42L, validationEnabled = false, occurredAt = OffsetDateTime.now()))

        verify(exactly = 1) { handler.handle(InitializeProjectValidationCommand(projectId = 42L, enabled = false)) }
    }

    @Test
    fun `validationEnabled=true — no-op (lazy ON, row 미생성)`() {
        listener.on(ProjectRegisteredEvent(projectId = 42L, validationEnabled = true, occurredAt = OffsetDateTime.now()))

        verify(exactly = 0) { handler.handle(any()) }
    }
}
