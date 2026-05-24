package com.docgraph.backend.graph.command.application

import com.docgraph.backend.graph.command.domain.DependencyEdgeNotFoundException
import com.docgraph.backend.graph.command.domain.DependencyEdgeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RemoveDependencyEdgeCommandHandler(
    private val edgeRepository: DependencyEdgeRepository,
) {
    @Transactional
    fun handle(command: RemoveDependencyEdgeCommand) {
        val edge = edgeRepository.findByProjectIdAndId(command.projectId, command.edgeId)
            ?: throw DependencyEdgeNotFoundException(command.edgeId)
        edgeRepository.delete(edge)
    }
}
