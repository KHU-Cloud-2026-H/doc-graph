package com.docgraph.backend.graph.command.application

import com.docgraph.backend.graph.command.domain.DependencyEdge
import com.docgraph.backend.graph.command.domain.DependencyEdgeNotFoundException
import com.docgraph.backend.graph.command.domain.DependencyEdgeRepository
import com.docgraph.backend.graph.command.domain.DependencyEdgeSource
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@Tag("unit")
class RemoveDependencyEdgeCommandHandlerTest {

    private val edgeRepository = mockk<DependencyEdgeRepository>(relaxUnitFun = true)
    private val handler = RemoveDependencyEdgeCommandHandler(edgeRepository)

    @Test
    fun `handle — edge 삭제`() {
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
    }

    @Test
    fun `handle — edge 없으면 DependencyEdgeNotFoundException`() {
        every { edgeRepository.findByProjectIdAndId(1L, 100L) } returns null

        assertThrows<DependencyEdgeNotFoundException> {
            handler.handle(RemoveDependencyEdgeCommand(projectId = 1L, edgeId = 100L))
        }
    }
}
