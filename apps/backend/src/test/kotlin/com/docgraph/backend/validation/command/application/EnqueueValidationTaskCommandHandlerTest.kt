package com.docgraph.backend.validation.command.application

import com.docgraph.backend.graph.query.application.EdgeDetail
import com.docgraph.backend.graph.query.application.FindEdgeByIdQuery
import com.docgraph.backend.validation.command.domain.ProjectValidationSetting
import com.docgraph.backend.validation.command.domain.ProjectValidationSettingRepository
import com.docgraph.backend.validation.command.domain.ValidationTask
import com.docgraph.backend.validation.command.domain.ValidationTaskQueuedEvent
import com.docgraph.backend.validation.command.domain.ValidationTaskRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.time.OffsetDateTime
import java.util.UUID

@Tag("unit")
class EnqueueValidationTaskCommandHandlerTest {

    private val repository = mockk<ValidationTaskRepository>(relaxed = true)
    private val settingRepository = mockk<ProjectValidationSettingRepository>()
    private val findEdgeById = mockk<FindEdgeByIdQuery>()
    private val publisher = mockk<ApplicationEventPublisher>(relaxUnitFun = true)

    private val handler = EnqueueValidationTaskCommandHandler(
        repository,
        settingRepository,
        findEdgeById,
        publisher,
    )

    private val projectId = 7L
    private val edgeId = 100L

    private fun command() = EnqueueValidationTaskCommand(UUID.randomUUID(), edgeId)

    private fun edge() = EdgeDetail(edgeId, projectId, 200L, 300L, "criterion")

    private fun savedTask() = ValidationTask(id = 1L, validationPairId = UUID.randomUUID(), edgeId = edgeId)

    private fun setting(enabled: Boolean) =
        ProjectValidationSetting(projectId = projectId, enabled = enabled, createdAt = OffsetDateTime.now())

    @Test
    fun `설정 없음(row 부재) — task 생성·발행`() {
        every { repository.findByValidationPairId(any()) } returns null
        every { findEdgeById.find(edgeId) } returns edge()
        every { settingRepository.findByProjectId(projectId) } returns null
        every { repository.save(any()) } returns savedTask()

        handler.handle(command())

        verify { repository.save(any()) }
        verify { publisher.publishEvent(any<ValidationTaskQueuedEvent>()) }
    }

    @Test
    fun `enabled=true — task 생성·발행`() {
        every { repository.findByValidationPairId(any()) } returns null
        every { findEdgeById.find(edgeId) } returns edge()
        every { settingRepository.findByProjectId(projectId) } returns setting(enabled = true)
        every { repository.save(any()) } returns savedTask()

        handler.handle(command())

        verify { repository.save(any()) }
        verify { publisher.publishEvent(any<ValidationTaskQueuedEvent>()) }
    }

    @Test
    fun `enabled=false — skip, task 미생성·미발행`() {
        every { repository.findByValidationPairId(any()) } returns null
        every { findEdgeById.find(edgeId) } returns edge()
        every { settingRepository.findByProjectId(projectId) } returns setting(enabled = false)

        handler.handle(command())

        verify(exactly = 0) { repository.save(any()) }
        verify(exactly = 0) { publisher.publishEvent(any<ValidationTaskQueuedEvent>()) }
    }

    @Test
    fun `edge 미해소 — fail-open으로 생성·발행 (setting 조회 안 함)`() {
        every { repository.findByValidationPairId(any()) } returns null
        every { findEdgeById.find(edgeId) } returns null
        every { repository.save(any()) } returns savedTask()

        handler.handle(command())

        verify(exactly = 0) { settingRepository.findByProjectId(any()) }
        verify { repository.save(any()) }
        verify { publisher.publishEvent(any<ValidationTaskQueuedEvent>()) }
    }

    @Test
    fun `이미 존재하는 pair — idempotent skip (게이트·생성 없음)`() {
        every { repository.findByValidationPairId(any()) } returns savedTask()

        handler.handle(command())

        verify(exactly = 0) { findEdgeById.find(any()) }
        verify(exactly = 0) { repository.save(any()) }
        verify(exactly = 0) { publisher.publishEvent(any<ValidationTaskQueuedEvent>()) }
    }
}