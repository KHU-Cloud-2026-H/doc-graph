package com.docgraph.backend.validation.command.application

import com.docgraph.backend.document.query.application.DocumentDetail
import com.docgraph.backend.document.query.application.DocumentType
import com.docgraph.backend.document.query.application.FindDocumentByIdQuery
import com.docgraph.backend.event.OutboxStatus
import com.docgraph.backend.graph.query.application.EdgeDetail
import com.docgraph.backend.graph.query.application.FindEdgeByIdQuery
import com.docgraph.backend.validation.command.domain.ValidationTaskPreparedEvent
import com.docgraph.backend.validation.command.domain.ValidationTask
import com.docgraph.backend.validation.command.domain.ValidationTaskRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.util.UUID
import kotlin.test.assertEquals

@Tag("unit")
class ProcessValidationTaskCommandHandlerTest {

    private val repository = mockk<ValidationTaskRepository>()
    private val transition = mockk<ValidationTaskTransitionService>(relaxUnitFun = true)
    private val findEdgeById = mockk<FindEdgeByIdQuery>()
    private val findDocumentById = mockk<FindDocumentByIdQuery>()
    private val publisher = mockk<ApplicationEventPublisher>(relaxUnitFun = true)

    private val handler = ProcessValidationTaskCommandHandler(
        repository,
        transition,
        findEdgeById,
        findDocumentById,
        publisher,
    )

    private fun pendingTask(taskId: Long, edgeId: Long): ValidationTask = ValidationTask(
        id = taskId,
        validationPairId = UUID.randomUUID(),
        edgeId = edgeId,
    )

    private fun document(id: Long): DocumentDetail =
        DocumentDetail(id, "page-$id", "title-$id", DocumentType.MEETING_NOTES, null, null, emptyList())

    @Test
    fun `정상 흐름 — recordAttempt + 두 문서 조회 + InputsResolved 발행`() {
        val taskId = 1L
        val edgeId = 100L
        every { repository.findById(taskId) } returns pendingTask(taskId, edgeId)
        every { findEdgeById.find(edgeId) } returns EdgeDetail(edgeId, 200L, 300L, "criterion")
        every { findDocumentById.find(any()) } returns document(0L)

        handler.handle(ProcessValidationTaskCommand(taskId))

        verify { transition.recordAttempt(taskId) }
        verify { findEdgeById.find(edgeId) }
        verify(exactly = 2) { findDocumentById.find(any()) }
        verify { publisher.publishEvent(any<ValidationTaskPreparedEvent>()) }
        verify(exactly = 0) { transition.markFailed(any(), any()) }
    }

    @Test
    fun `task가 PENDING이 아니면 즉시 return — query 호출 없음`() {
        val taskId = 1L
        val task = pendingTask(taskId, 100L).apply { status = OutboxStatus.SUCCESS }
        every { repository.findById(taskId) } returns task

        handler.handle(ProcessValidationTaskCommand(taskId))

        verify { transition.recordAttempt(taskId) }
        verify(exactly = 0) { findEdgeById.find(any()) }
        verify(exactly = 0) { publisher.publishEvent(any<ValidationTaskPreparedEvent>()) }
    }

    @Test
    fun `findEdgeById null — markFailed 즉시, retry 없음`() {
        val taskId = 1L
        val edgeId = 100L
        every { repository.findById(taskId) } returns pendingTask(taskId, edgeId)
        every { findEdgeById.find(edgeId) } returns null

        handler.handle(ProcessValidationTaskCommand(taskId))

        verify { transition.markFailed(taskId, match { it != null && it.contains("edge not found") }) }
        verify(exactly = 0) { findDocumentById.find(any()) }
        verify(exactly = 0) { publisher.publishEvent(any<ValidationTaskPreparedEvent>()) }
    }

    @Test
    fun `source document null — markFailed 즉시`() {
        val taskId = 1L
        val edgeId = 100L
        every { repository.findById(taskId) } returns pendingTask(taskId, edgeId)
        every { findEdgeById.find(edgeId) } returns EdgeDetail(edgeId, 200L, 300L, "criterion")
        every { findDocumentById.find(200L) } returns null

        handler.handle(ProcessValidationTaskCommand(taskId))

        verify { transition.markFailed(taskId, match { it != null && it.contains("source") }) }
        verify(exactly = 0) { publisher.publishEvent(any<ValidationTaskPreparedEvent>()) }
    }

    @Test
    fun `target document null — markFailed 즉시`() {
        val taskId = 1L
        val edgeId = 100L
        every { repository.findById(taskId) } returns pendingTask(taskId, edgeId)
        every { findEdgeById.find(edgeId) } returns EdgeDetail(edgeId, 200L, 300L, "criterion")
        every { findDocumentById.find(200L) } returns document(200L)
        every { findDocumentById.find(300L) } returns null

        handler.handle(ProcessValidationTaskCommand(taskId))

        verify { transition.markFailed(taskId, match { it != null && it.contains("target") }) }
        verify(exactly = 0) { publisher.publishEvent(any<ValidationTaskPreparedEvent>()) }
    }

    @Test
    fun `findEdgeById throw — handler가 rethrow (Retryable이 잡음)`() {
        val taskId = 1L
        val edgeId = 100L
        every { repository.findById(taskId) } returns pendingTask(taskId, edgeId)
        every { findEdgeById.find(edgeId) } throws RuntimeException("transient")

        val ex = assertThrows(RuntimeException::class.java) {
            handler.handle(ProcessValidationTaskCommand(taskId))
        }
        assertEquals("transient", ex.message)
        verify(exactly = 0) { transition.markFailed(any(), any()) }
        verify(exactly = 0) { publisher.publishEvent(any<ValidationTaskPreparedEvent>()) }
    }

    @Test
    fun `recover — markFailed with throwable message`() {
        val ex = RuntimeException("retry exhausted")

        handler.recover(ex, ProcessValidationTaskCommand(1L))

        verify { transition.markFailed(1L, "retry exhausted") }
    }
}
