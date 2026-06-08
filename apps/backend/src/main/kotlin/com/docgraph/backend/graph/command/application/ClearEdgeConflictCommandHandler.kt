package com.docgraph.backend.graph.command.application

import com.docgraph.backend.graph.command.domain.DependencyEdgeNotFoundException
import com.docgraph.backend.graph.command.domain.DependencyEdgeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class ClearEdgeConflictCommandHandler(
    private val edgeRepository: DependencyEdgeRepository,
) {
    // ConflictResolvedEventListener(AFTER_COMMIT)에서 호출 — afterCommit 단계의 REQUIRED는 커밋되지 않으므로
    // REQUIRES_NEW로 독립 트랜잭션을 연다. (MarkEdgeConflictCommandHandler와 동일 이유)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(command: ClearEdgeConflictCommand) {
        val edge = edgeRepository.findById(command.edgeId)
            ?: throw DependencyEdgeNotFoundException(command.edgeId)

        edge.clearConflict(command.occurredAt)
        edgeRepository.save(edge)
    }
}
