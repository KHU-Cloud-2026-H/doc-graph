package com.docgraph.backend.graph.command.application

import com.docgraph.backend.graph.command.domain.DependencyEdge
import com.docgraph.backend.graph.command.domain.DependencyEdgeRepository
import com.docgraph.backend.graph.command.domain.DependencyEdgeSource
import com.docgraph.backend.graph.command.domain.EdgeProposal
import com.docgraph.backend.graph.command.domain.EdgeProposalNotFoundException
import com.docgraph.backend.graph.command.domain.EdgeProposalRepository
import com.docgraph.backend.graph.command.domain.ValidationPairCreatedEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.util.UUID

@Tag("unit")
class AcceptEdgeProposalCommandHandlerTest {

    private val proposalRepository = mockk<EdgeProposalRepository>(relaxUnitFun = true)
    private val edgeRepository = mockk<DependencyEdgeRepository>()
    private val publisher = mockk<ApplicationEventPublisher>(relaxUnitFun = true)
    private val handler = AcceptEdgeProposalCommandHandler(proposalRepository, edgeRepository, publisher)

    @Test
    fun `handle — proposal을 edge로 전환하고 ValidationPairCreatedEvent 발행`() {
        val validationPairId = UUID.randomUUID()
        val proposal = proposal()
        val capturedEdge = slot<DependencyEdge>()
        val capturedEvent = slot<ValidationPairCreatedEvent>()
        every { proposalRepository.findByProjectIdAndId(1L, 50L) } returns proposal
        every {
            edgeRepository.findByProjectIdAndSourceDocumentIdAndTargetDocumentId(1L, 10L, 20L)
        } returns null
        every { edgeRepository.save(capture(capturedEdge)) } answers {
            capturedEdge.captured.apply { id = 100L }
        }
        every { publisher.publishEvent(capture(capturedEvent)) } returns Unit

        val edge = handler.handle(
            AcceptEdgeProposalCommand(
                projectId = 1L,
                proposalId = 50L,
                validationPairId = validationPairId,
            ),
        )

        assertEquals(100L, edge.id)
        assertEquals(DependencyEdgeSource.PROPOSAL_ACCEPTED, edge.source)
        assertEquals(validationPairId, capturedEvent.captured.validationPairId)
        assertEquals(100L, capturedEvent.captured.edgeId)
        verify { proposalRepository.delete(proposal) }
        verify { publisher.publishEvent(any<ValidationPairCreatedEvent>()) }
    }

    @Test
    fun `handle — 같은 방향 edge가 이미 있으면 proposal만 삭제하고 이벤트 발행 생략`() {
        val proposal = proposal()
        val existing = DependencyEdge(
            id = 100L,
            projectId = 1L,
            sourceDocumentId = 10L,
            targetDocumentId = 20L,
            validationCriterion = "결정사항 반영 여부",
            source = DependencyEdgeSource.NOTION_REFERENCE,
        )
        every { proposalRepository.findByProjectIdAndId(1L, 50L) } returns proposal
        every {
            edgeRepository.findByProjectIdAndSourceDocumentIdAndTargetDocumentId(1L, 10L, 20L)
        } returns existing

        val edge = handler.handle(AcceptEdgeProposalCommand(projectId = 1L, proposalId = 50L))

        assertEquals(existing, edge)
        verify { proposalRepository.delete(proposal) }
        verify(exactly = 0) { edgeRepository.save(any()) }
        verify(exactly = 0) { publisher.publishEvent(any<ValidationPairCreatedEvent>()) }
    }

    @Test
    fun `handle — proposal이 없으면 EdgeProposalNotFoundException`() {
        every { proposalRepository.findByProjectIdAndId(1L, 50L) } returns null

        assertThrows(EdgeProposalNotFoundException::class.java) {
            handler.handle(AcceptEdgeProposalCommand(projectId = 1L, proposalId = 50L))
        }
    }

    private fun proposal(): EdgeProposal =
        EdgeProposal(
            id = 50L,
            projectId = 1L,
            sourceDocumentId = 10L,
            targetDocumentId = 20L,
            ruleId = 30L,
            validationCriterion = "결정사항 반영 여부",
            similarityScore = 0.82,
        )
}
