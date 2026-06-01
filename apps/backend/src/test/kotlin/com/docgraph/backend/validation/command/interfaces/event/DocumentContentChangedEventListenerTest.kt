package com.docgraph.backend.validation.command.interfaces.event

import com.docgraph.backend.document.command.domain.DocumentContentChangedEvent
import com.docgraph.backend.graph.command.domain.DependencyEdge
import com.docgraph.backend.graph.command.domain.DependencyEdgeRepository
import com.docgraph.backend.validation.command.application.EnqueueValidationTaskCommand
import com.docgraph.backend.validation.command.application.EnqueueValidationTaskCommandHandler
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@Tag("unit")
class DocumentContentChangedEventListenerTest {

    private val edgeRepository = mockk<DependencyEdgeRepository>()
    private val enqueueHandler = mockk<EnqueueValidationTaskCommandHandler>(relaxed = true)
    private val listener = DocumentContentChangedEventListener(edgeRepository, enqueueHandler)

    @Test
    fun `소스 문서 변경 → 해당 문서가 source인 엣지마다 ValidationTask 등록`() {
        every { edgeRepository.findAllBySourceDocumentId(99L) } returns listOf(edge(10L), edge(20L))

        listener.on(event(documentId = 99L))

        verify(exactly = 2) { enqueueHandler.handle(any()) }
    }

    @Test
    fun `해당 소스 문서가 source인 엣지 없음 → enqueue 호출 안 함`() {
        every { edgeRepository.findAllBySourceDocumentId(99L) } returns emptyList()

        listener.on(event(documentId = 99L))

        verify(exactly = 0) { enqueueHandler.handle(any()) }
    }

    @Test
    fun `엣지별로 서로 다른 validationPairId 사용`() {
        every { edgeRepository.findAllBySourceDocumentId(99L) } returns listOf(edge(10L), edge(20L))
        val ids = mutableListOf<EnqueueValidationTaskCommand>()
        every { enqueueHandler.handle(any()) } answers { ids.add(firstArg()) }

        listener.on(event(documentId = 99L))

        assertEquals(2, ids.size)
        assertNotEquals(ids[0].validationPairId, ids[1].validationPairId, "엣지마다 고유한 UUID 사용")
        assertEquals(10L, ids[0].edgeId)
        assertEquals(20L, ids[1].edgeId)
    }

    private fun event(documentId: Long) =
        DocumentContentChangedEvent(documentId = documentId, projectId = 1L, notionPageId = "page-x")

    private fun edge(id: Long): DependencyEdge = mockk {
        every { this@mockk.id } returns id
    }
}
