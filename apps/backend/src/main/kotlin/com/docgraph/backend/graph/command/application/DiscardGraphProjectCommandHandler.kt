package com.docgraph.backend.graph.command.application

import com.docgraph.backend.graph.command.domain.DependencyEdgeRepository
import com.docgraph.backend.graph.command.domain.EdgeProposalRepository
import com.docgraph.backend.graph.command.domain.GraphRuleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DiscardGraphProjectCommandHandler(
    private val edgeRepository: DependencyEdgeRepository,
    private val proposalRepository: EdgeProposalRepository,
    private val ruleRepository: GraphRuleRepository,
) {
    @Transactional
    fun handle(command: DiscardGraphProjectCommand) {
        proposalRepository.deleteAllByProjectId(command.projectId)
        edgeRepository.deleteAllByProjectId(command.projectId)
        ruleRepository.deleteAllByProjectId(command.projectId)
    }
}
