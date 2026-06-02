package com.docgraph.backend.graph.command.application

import com.docgraph.backend.graph.command.domain.DependencyEdge
import com.docgraph.backend.graph.command.domain.DependencyEdgeRepository
import com.docgraph.backend.graph.command.domain.DependencyEdgeSource
import com.docgraph.backend.graph.command.domain.EdgeProposal
import com.docgraph.backend.graph.command.domain.EdgeProposalRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("unit")
class ProposeEdgeCommandHandlerTest {

    private val proposalRepository = mockk<EdgeProposalRepository>(relaxUnitFun = true)
    private val edgeRepository = mockk<DependencyEdgeRepository>()
    private val handler = ProposeEdgeCommandHandler(proposalRepository, edgeRepository)

    private fun command() = ProposeEdgeCommand(
        projectId = 1L,
        sourceDocumentId = 10L,
        targetDocumentId = 20L,
        ruleId = 30L,
        validationCriterion = "범위 일치 여부",
        similarityScore = 0.42,
    )

    @Test
    fun `handle — 신규 쌍은 제안으로 저장`() {
        every { edgeRepository.findByProjectIdAndSourceDocumentIdAndTargetDocumentId(1L, 10L, 20L) } returns null
        every { proposalRepository.findByProjectIdAndSourceDocumentIdAndTargetDocumentId(1L, 10L, 20L) } returns null
        val saved = slot<EdgeProposal>()
        every { proposalRepository.save(capture(saved)) } answers { saved.captured }

        val result = handler.handle(command())

        verify { proposalRepository.save(any()) }
        assertEquals(10L, saved.captured.sourceDocumentId)
        assertEquals(20L, saved.captured.targetDocumentId)
        assertEquals(30L, saved.captured.ruleId)
        assertEquals("범위 일치 여부", saved.captured.validationCriterion)
        assertEquals(0.42, saved.captured.similarityScore)
        assertSame(saved.captured, result)
    }

    @Test
    fun `handle — 이미 동일 방향 엣지가 있으면 제안하지 않음`() {
        every { edgeRepository.findByProjectIdAndSourceDocumentIdAndTargetDocumentId(1L, 10L, 20L) } returns
            DependencyEdge(
                projectId = 1L,
                sourceDocumentId = 10L,
                targetDocumentId = 20L,
                validationCriterion = "범위 일치 여부",
                source = DependencyEdgeSource.NOTION_REFERENCE,
            )

        val result = handler.handle(command())

        assertNull(result)
        verify(exactly = 0) { proposalRepository.save(any()) }
    }

    @Test
    fun `handle — 이미 동일 방향 제안이 있으면 기존을 유지`() {
        val existing = EdgeProposal(
            id = 99L,
            projectId = 1L,
            sourceDocumentId = 10L,
            targetDocumentId = 20L,
            validationCriterion = "범위 일치 여부",
            similarityScore = 0.91,
        )
        every { edgeRepository.findByProjectIdAndSourceDocumentIdAndTargetDocumentId(1L, 10L, 20L) } returns null
        every { proposalRepository.findByProjectIdAndSourceDocumentIdAndTargetDocumentId(1L, 10L, 20L) } returns existing

        val result = handler.handle(command())

        assertSame(existing, result)
        verify(exactly = 0) { proposalRepository.save(any()) }
    }
}
