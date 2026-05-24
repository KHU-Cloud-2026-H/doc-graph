package com.docgraph.backend.graph.command.application

import com.docgraph.backend.graph.command.domain.DependencyEdgeRepository
import com.docgraph.backend.graph.command.domain.EdgeProposalRepository
import com.docgraph.backend.graph.command.domain.GraphRuleRepository
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("unit")
class DiscardGraphProjectCommandHandlerTest {

    private val edgeRepository = mockk<DependencyEdgeRepository>(relaxUnitFun = true)
    private val proposalRepository = mockk<EdgeProposalRepository>(relaxUnitFun = true)
    private val ruleRepository = mockk<GraphRuleRepository>(relaxUnitFun = true)
    private val handler = DiscardGraphProjectCommandHandler(edgeRepository, proposalRepository, ruleRepository)

    @Test
    fun `handle — 프로젝트 소속 graph row 정리`() {
        handler.handle(DiscardGraphProjectCommand(projectId = 1L))

        verify { proposalRepository.deleteAllByProjectId(1L) }
        verify { edgeRepository.deleteAllByProjectId(1L) }
        verify { ruleRepository.deleteAllByProjectId(1L) }
    }
}
