package com.docgraph.backend.graph.command.application

import com.docgraph.backend.graph.command.domain.DependencyEdgeNotFoundException
import com.docgraph.backend.graph.command.domain.DependencyEdgeRemovedEvent
import com.docgraph.backend.graph.command.domain.DependencyEdgeRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class RemoveDependencyEdgeCommandHandler(
    private val edgeRepository: DependencyEdgeRepository,
    private val publisher: ApplicationEventPublisher,
) {
    @Transactional
    fun handle(command: RemoveDependencyEdgeCommand) {
        val edge = edgeRepository.findByProjectIdAndId(command.projectId, command.edgeId)
            ?: throw DependencyEdgeNotFoundException(command.edgeId)
        edgeRepository.delete(edge)
        publisher.publishEvent(
            DependencyEdgeRemovedEvent(
                edgeId = edge.id,
                projectId = edge.projectId,
                occurredAt = OffsetDateTime.now(),
            ),
        )
    }
}
