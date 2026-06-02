package com.docgraph.backend.validation.query.application

import com.docgraph.backend.project.query.application.SearchAdminProjectIdsByUserIdQuery
import com.docgraph.backend.validation.command.domain.ProjectValidationSetting
import com.docgraph.backend.validation.command.domain.ProjectValidationSettingRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

@Tag("unit")
class FindProjectValidationQueryHandlerTest {

    private val settingRepository = mockk<ProjectValidationSettingRepository>()
    private val searchAdminProjectIds = mockk<SearchAdminProjectIdsByUserIdQuery>()
    private val handler = FindProjectValidationQueryHandler(settingRepository, searchAdminProjectIds)

    @Test
    fun `Admin + setting 있음 — 저장된 enabled 반환`() {
        every { searchAdminProjectIds.search(7L) } returns listOf(10L)
        every { settingRepository.findByProjectId(10L) } returns
            ProjectValidationSetting(projectId = 10L, enabled = false, createdAt = OffsetDateTime.now())

        assertEquals(false, handler.find(10L, 7L)?.enabled)
    }

    @Test
    fun `Admin + setting 없음 — 기본 enabled=true`() {
        every { searchAdminProjectIds.search(7L) } returns listOf(10L)
        every { settingRepository.findByProjectId(10L) } returns null

        assertEquals(true, handler.find(10L, 7L)?.enabled)
    }

    @Test
    fun `비Admin — null (조회 권한 없음)`() {
        every { searchAdminProjectIds.search(7L) } returns listOf(99L)

        assertNull(handler.find(10L, 7L))
    }
}
