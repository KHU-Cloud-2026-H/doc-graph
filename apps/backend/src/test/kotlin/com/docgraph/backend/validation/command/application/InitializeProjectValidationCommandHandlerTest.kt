package com.docgraph.backend.validation.command.application

import com.docgraph.backend.validation.command.domain.ProjectValidationSetting
import com.docgraph.backend.validation.command.domain.ProjectValidationSettingRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@Tag("unit")
class InitializeProjectValidationCommandHandlerTest {

    private val repository = mockk<ProjectValidationSettingRepository>()
    private val handler = InitializeProjectValidationCommandHandler(repository)

    @Test
    fun `setting 영속 — admin 게이트 없이 enabled 값으로 저장`() {
        val captured = slot<ProjectValidationSetting>()
        every { repository.save(capture(captured)) } answers { captured.captured.also { it.id = 1L } }

        handler.handle(InitializeProjectValidationCommand(projectId = 42L, enabled = false))

        assertEquals(42L, captured.captured.projectId)
        assertFalse(captured.captured.enabled)
        verify(exactly = 1) { repository.save(any()) }
    }
}
