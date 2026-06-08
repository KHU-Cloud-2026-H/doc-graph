package com.docgraph.backend.graph.command.application

import com.docgraph.backend.graph.command.domain.DependencyEdgeNotFoundException
import com.docgraph.backend.graph.command.domain.DependencyEdgeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class MarkEdgeConflictCommandHandler(
    private val edgeRepository: DependencyEdgeRepository,
) {
    // ConflictDetectedEventListener(AFTER_COMMIT)에서 호출 — 이미 커밋된 트랜잭션의 afterCommit 단계라
    // REQUIRED로는 새 커밋이 일어나지 않아 마킹이 소실된다. REQUIRES_NEW로 독립 트랜잭션을 연다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(command: MarkEdgeConflictCommand) {
        val edge = edgeRepository.findById(command.edgeId)
            ?: throw DependencyEdgeNotFoundException(command.edgeId)

        edge.markConflict(command.occurredAt)
        edgeRepository.save(edge)
    }
}
