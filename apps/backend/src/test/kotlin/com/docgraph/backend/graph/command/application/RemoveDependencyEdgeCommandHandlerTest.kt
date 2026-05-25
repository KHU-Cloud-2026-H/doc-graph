package com.docgraph.backend.graph.command.application

import com.docgraph.backend.graph.command.domain.DependencyEdge
import com.docgraph.backend.graph.command.domain.DependencyEdgeNotFoundException
import com.docgraph.backend.graph.command.domain.DependencyEdgeRemovedEvent
import com.docgraph.backend.graph.command.domain.DependencyEdgeRepository
import com.docgraph.backend.graph.command.domain.DependencyEdgeSource
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.ApplicationEventPublisher

@Tag("unit")
class RemoveDependencyEdgeCommandHandlerTest {

    private val edgeRepository = mockk<DependencyEdgeRepository>(relaxUnitFun = true)
    private val publisher = mockk<ApplicationEventPublisher>(relaxed = true)
    private val handler = RemoveDependencyEdgeCommandHandler(edgeRepository, publisher)

    @Test
    fun `handle — edge 삭제 + DependencyEdgeRemovedEvent 발행`() {
        val edge = DependencyEdge(
            id = 100L,
            projectId = 1L,
            sourceDocumentId = 10L,
            targetDocumentId = 20L,
            validationCriterion = "범위 일치 여부",
            source = DependencyEdgeSource.CUSTOM,
        )
        every { edgeRepository.findByProjectIdAndId(1L, 100L) } returns edge

        handler.handle(RemoveDependencyEdgeCommand(projectId = 1L, edgeId = 100L))

        verify { edgeRepository.delete(edge) }
        verify {
            publisher.publishEvent(match<DependencyEdgeRemovedEvent> { it.edgeId == 100L && it.projectId == 1L })
        }
    }

    @Test
    fun `handle — edge 없으면 DependencyEdgeNotFoundException, 이벤트 발행 X`() {
        every { edgeRepository.findByProjectIdAndId(1L, 100L) } returns null

        assertThrows<DependencyEdgeNotFoundException> {
            handler.handle(RemoveDependencyEdgeCommand(projectId = 1L, edgeId = 100L))
        }
        verify(exactly = 0) { publisher.publishEvent(any<DependencyEdgeRemovedEvent>()) }
    }
}
