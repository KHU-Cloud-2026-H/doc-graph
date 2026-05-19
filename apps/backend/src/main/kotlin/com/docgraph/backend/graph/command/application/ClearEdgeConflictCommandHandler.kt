package com.docgraph.backend.graph.command.application

import com.docgraph.backend.graph.command.domain.DependencyEdgeNotFoundException
import com.docgraph.backend.graph.command.domain.DependencyEdgeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ClearEdgeConflictCommandHandler(
    private val edgeRepository: DependencyEdgeRepository,
) {
    @Transactional
    fun handle(command: ClearEdgeConflictCommand) {
        val edge = edgeRepository.findById(command.edgeId)
            ?: throw DependencyEdgeNotFoundException(command.edgeId)

        edge.clearConflict(command.occurredAt)
        edgeRepository.save(edge)
    }
}
