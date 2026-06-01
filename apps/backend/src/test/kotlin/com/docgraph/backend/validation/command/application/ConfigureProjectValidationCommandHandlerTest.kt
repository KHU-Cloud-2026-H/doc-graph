package com.docgraph.backend.validation.command.application

import com.docgraph.backend.project.query.application.SearchAdminProjectIdsByUserIdQuery
import com.docgraph.backend.validation.command.domain.ProjectValidationSetting
import com.docgraph.backend.validation.command.domain.ProjectValidationSettingRepository
import com.docgraph.backend.validation.command.domain.ValidationPermissionDeniedException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@Tag("unit")
class ConfigureProjectValidationCommandHandlerTest {

    private val repository = mockk<ProjectValidationSettingRepository>()
    private val searchAdminProjectIds = mockk<SearchAdminProjectIdsByUserIdQuery>()
    private val handler = ConfigureProjectValidationCommandHandler(repository, searchAdminProjectIds)

    @Test
    fun `Admin 프로젝트 + 미설정 — 새 setting save`() {
        every { searchAdminProjectIds.search(7L) } returns listOf(10L, 20L)
        every { repository.findByProjectId(10L) } returns null
        val captured = slot<ProjectValidationSetting>()
        every { repository.save(capture(captured)) } answers { captured.captured.also { it.id = 1L } }

        handler.handle(ConfigureProjectValidationCommand(projectId = 10L, requesterUserId = 7L, enabled = false))

        assertEquals(10L, captured.captured.projectId)
        assertFalse(captured.captured.enabled)
        verify(exactly = 1) { repository.save(any()) }
    }

    @Test
    fun `Admin 프로젝트 + 기존 — 기존 row enabled 갱신, 신규 insert 없음`() {
        val existing = ProjectValidationSetting(id = 5L, projectId = 10L, enabled = true, createdAt = OffsetDateTime.now())
        every { searchAdminProjectIds.search(7L) } returns listOf(10L)
        every { repository.findByProjectId(10L) } returns existing
        every { repository.save(existing) } returns existing

        handler.handle(ConfigureProjectValidationCommand(projectId = 10L, requesterUserId = 7L, enabled = false))

        assertFalse(existing.enabled)
    }

    @Test
    fun `비Admin 프로젝트 — ValidationPermissionDeniedException, save 호출 없음`() {
        every { searchAdminProjectIds.search(7L) } returns listOf(99L)

        assertThrows<ValidationPermissionDeniedException> {
            handler.handle(ConfigureProjectValidationCommand(projectId = 10L, requesterUserId = 7L, enabled = false))
        }
        verify(exactly = 0) { repository.save(any()) }
    }
}
