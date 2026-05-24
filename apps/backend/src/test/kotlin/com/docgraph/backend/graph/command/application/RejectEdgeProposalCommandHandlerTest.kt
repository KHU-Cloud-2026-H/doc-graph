package com.docgraph.backend.graph.command.application

import com.docgraph.backend.graph.command.domain.EdgeProposal
import com.docgraph.backend.graph.command.domain.EdgeProposalNotFoundException
import com.docgraph.backend.graph.command.domain.EdgeProposalRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@Tag("unit")
class RejectEdgeProposalCommandHandlerTest {

    private val proposalRepository = mockk<EdgeProposalRepository>(relaxUnitFun = true)
    private val handler = RejectEdgeProposalCommandHandler(proposalRepository)

    @Test
    fun `handle — proposal 삭제`() {
        val proposal = EdgeProposal(
            id = 200L,
            projectId = 1L,
            sourceDocumentId = 10L,
            targetDocumentId = 20L,
            validationCriterion = "범위 일치 여부",
            similarityScore = 0.87,
        )
        every { proposalRepository.findByProjectIdAndId(1L, 200L) } returns proposal

        handler.handle(RejectEdgeProposalCommand(projectId = 1L, proposalId = 200L))

        verify { proposalRepository.delete(proposal) }
    }

    @Test
    fun `handle — proposal 없으면 EdgeProposalNotFoundException`() {
        every { proposalRepository.findByProjectIdAndId(1L, 200L) } returns null

        assertThrows<EdgeProposalNotFoundException> {
            handler.handle(RejectEdgeProposalCommand(projectId = 1L, proposalId = 200L))
        }
    }
}
