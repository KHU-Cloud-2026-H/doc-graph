package com.docgraph.backend.graph.command.application

import com.docgraph.backend.graph.command.domain.EdgeProposalNotFoundException
import com.docgraph.backend.graph.command.domain.EdgeProposalRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RejectEdgeProposalCommandHandler(
    private val proposalRepository: EdgeProposalRepository,
) {
    @Transactional
    fun handle(command: RejectEdgeProposalCommand) {
        val proposal = proposalRepository.findByProjectIdAndId(command.projectId, command.proposalId)
            ?: throw EdgeProposalNotFoundException(command.proposalId)
        proposalRepository.delete(proposal)
    }
}
