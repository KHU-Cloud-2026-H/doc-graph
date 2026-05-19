package com.docgraph.backend.graph.command.application

import com.docgraph.backend.graph.command.domain.DependencyEdge
import com.docgraph.backend.graph.command.domain.DependencyEdgeRepository
import com.docgraph.backend.graph.command.domain.DependencyEdgeSource
import com.docgraph.backend.graph.command.domain.ValidationPairCreatedEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.util.UUID

@Tag("unit")
class CreateDependencyEdgeCommandHandlerTest {

    private val edgeRepository = mockk<DependencyEdgeRepository>()
    private val publisher = mockk<ApplicationEventPublisher>(relaxUnitFun = true)
    private val handler = CreateDependencyEdgeCommandHandler(edgeRepository, publisher)

    @Test
    fun `handle — 새 edge 저장 후 ValidationPairCreatedEvent 발행`() {
        val validationPairId = UUID.randomUUID()
        val capturedEdge = slot<DependencyEdge>()
        val capturedEvent = slot<ValidationPairCreatedEvent>()
        every {
            edgeRepository.findByProjectIdAndSourceDocumentIdAndTargetDocumentId(1L, 10L, 20L)
        } returns null
        every { edgeRepository.save(capture(capturedEdge)) } answers {
            capturedEdge.captured.apply { id = 100L }
        }
        every { publisher.publishEvent(capture(capturedEvent)) } returns Unit

        val edge = handler.handle(
            CreateDependencyEdgeCommand(
                projectId = 1L,
                sourceDocumentId = 10L,
                targetDocumentId = 20L,
                ruleId = 30L,
                validationCriterion = "범위 일치 여부",
                source = DependencyEdgeSource.NOTION_REFERENCE,
                validationPairId = validationPairId,
            ),
        )

        assertEquals(100L, edge.id)
        assertEquals(1L, edge.projectId)
        assertEquals(10L, edge.sourceDocumentId)
        assertEquals(20L, edge.targetDocumentId)
        assertEquals(DependencyEdgeSource.NOTION_REFERENCE, edge.source)
        assertEquals(validationPairId, capturedEvent.captured.validationPairId)
        assertEquals(100L, capturedEvent.captured.edgeId)
        verify { publisher.publishEvent(any<ValidationPairCreatedEvent>()) }
    }

    @Test
    fun `handle — 같은 방향 edge가 이미 있으면 저장과 이벤트 발행 생략`() {
        val existing = DependencyEdge(
            id = 100L,
            projectId = 1L,
            sourceDocumentId = 10L,
            targetDocumentId = 20L,
            validationCriterion = "범위 일치 여부",
            source = DependencyEdgeSource.NOTION_REFERENCE,
        )
        every {
            edgeRepository.findByProjectIdAndSourceDocumentIdAndTargetDocumentId(1L, 10L, 20L)
        } returns existing

        val edge = handler.handle(
            CreateDependencyEdgeCommand(
                projectId = 1L,
                sourceDocumentId = 10L,
                targetDocumentId = 20L,
                ruleId = null,
                validationCriterion = "범위 일치 여부",
                source = DependencyEdgeSource.NOTION_REFERENCE,
            ),
        )

        assertEquals(existing, edge)
        verify(exactly = 0) { edgeRepository.save(any()) }
        verify(exactly = 0) { publisher.publishEvent(any<ValidationPairCreatedEvent>()) }
    }
}
