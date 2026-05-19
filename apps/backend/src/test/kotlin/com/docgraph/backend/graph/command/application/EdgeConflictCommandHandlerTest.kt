package com.docgraph.backend.graph.command.application

import com.docgraph.backend.graph.command.domain.DependencyEdge
import com.docgraph.backend.graph.command.domain.DependencyEdgeNotFoundException
import com.docgraph.backend.graph.command.domain.DependencyEdgeRepository
import com.docgraph.backend.graph.command.domain.DependencyEdgeSource
import com.docgraph.backend.graph.command.domain.EdgeConflictStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

@Tag("unit")
class EdgeConflictCommandHandlerTest {

    private val edgeRepository = mockk<DependencyEdgeRepository>()
    private val markHandler = MarkEdgeConflictCommandHandler(edgeRepository)
    private val clearHandler = ClearEdgeConflictCommandHandler(edgeRepository)

    @Test
    fun `mark — edge를 CONFLICT 상태로 전환`() {
        val occurredAt = OffsetDateTime.parse("2026-05-20T10:00:00+09:00")
        val edge = edge()
        val captured = slot<DependencyEdge>()
        every { edgeRepository.findById(100L) } returns edge
        every { edgeRepository.save(capture(captured)) } answers { captured.captured }

        markHandler.handle(MarkEdgeConflictCommand(edgeId = 100L, occurredAt = occurredAt))

        assertEquals(EdgeConflictStatus.CONFLICT, captured.captured.conflictStatus)
        assertEquals(occurredAt, captured.captured.updatedAt)
    }

    @Test
    fun `clear — edge를 NONE 상태로 복원`() {
        val occurredAt = OffsetDateTime.parse("2026-05-20T11:00:00+09:00")
        val edge = edge().apply {
            markConflict(OffsetDateTime.parse("2026-05-20T10:00:00+09:00"))
        }
        val captured = slot<DependencyEdge>()
        every { edgeRepository.findById(100L) } returns edge
        every { edgeRepository.save(capture(captured)) } answers { captured.captured }

        clearHandler.handle(ClearEdgeConflictCommand(edgeId = 100L, occurredAt = occurredAt))

        assertEquals(EdgeConflictStatus.NONE, captured.captured.conflictStatus)
        assertEquals(occurredAt, captured.captured.updatedAt)
    }

    @Test
    fun `mark — edge가 없으면 DependencyEdgeNotFoundException`() {
        every { edgeRepository.findById(100L) } returns null

        assertThrows(DependencyEdgeNotFoundException::class.java) {
            markHandler.handle(
                MarkEdgeConflictCommand(
                    edgeId = 100L,
                    occurredAt = OffsetDateTime.parse("2026-05-20T10:00:00+09:00"),
                ),
            )
        }

        verify(exactly = 0) { edgeRepository.save(any()) }
    }

    private fun edge(): DependencyEdge =
        DependencyEdge(
            id = 100L,
            projectId = 1L,
            sourceDocumentId = 10L,
            targetDocumentId = 20L,
            validationCriterion = "범위 일치 여부",
            source = DependencyEdgeSource.NOTION_REFERENCE,
        )
}
