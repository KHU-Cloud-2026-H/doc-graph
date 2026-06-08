package com.docgraph.backend.graph.command.application

import com.docgraph.backend.graph.command.domain.DependencyEdgeRepository
import com.docgraph.backend.graph.command.domain.EdgeProposalRepository
import com.docgraph.backend.graph.command.domain.GraphRuleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class DiscardGraphProjectCommandHandler(
    private val edgeRepository: DependencyEdgeRepository,
    private val proposalRepository: EdgeProposalRepository,
    private val ruleRepository: GraphRuleRepository,
) {
    // ProjectDiscardedEventListener(AFTER_COMMIT)에서 호출 — afterCommit 단계의 REQUIRED는 커밋되지 않으므로
    // REQUIRES_NEW로 독립 트랜잭션을 연다. (Mark/ClearEdgeConflictCommandHandler와 동일 이유)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(command: DiscardGraphProjectCommand) {
        proposalRepository.deleteAllByProjectId(command.projectId)
        edgeRepository.deleteAllByProjectId(command.projectId)
        ruleRepository.deleteAllByProjectId(command.projectId)
    }
}
