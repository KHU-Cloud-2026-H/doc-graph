package com.docgraph.backend.validation.command.application

import com.docgraph.backend.validation.command.domain.AiUsageRecordRepository
import com.docgraph.backend.validation.command.domain.ConflictRepository
import com.docgraph.backend.validation.command.domain.ProjectValidationSettingRepository
import com.docgraph.backend.validation.command.domain.ValidationTaskRepository
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("unit")
class DiscardProjectValidationCommandHandlerTest {

    private val taskRepository = mockk<ValidationTaskRepository>(relaxUnitFun = true)
    private val conflictRepository = mockk<ConflictRepository>(relaxUnitFun = true)
    private val aiUsageRecordRepository = mockk<AiUsageRecordRepository>(relaxUnitFun = true)
    private val settingRepository = mockk<ProjectValidationSettingRepository>(relaxUnitFun = true)
    private val handler = DiscardProjectValidationCommandHandler(
        taskRepository,
        conflictRepository,
        aiUsageRecordRepository,
        settingRepository,
    )

    @Test
    fun `handle — 프로젝트 소속 validation row 전부 정리`() {
        handler.handle(DiscardProjectValidationCommand(projectId = 1L))

        // conflict_finding은 task·conflict 양쪽 ON DELETE CASCADE라 명시 삭제 불필요.
        verify { taskRepository.deleteByProjectId(1L) }
        verify { conflictRepository.deleteByProjectId(1L) }
        verify { aiUsageRecordRepository.deleteByProjectId(1L) }
        verify { settingRepository.deleteByProjectId(1L) }
    }
}
