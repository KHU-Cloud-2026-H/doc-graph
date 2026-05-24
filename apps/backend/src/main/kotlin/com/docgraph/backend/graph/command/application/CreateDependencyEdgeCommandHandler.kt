package com.docgraph.backend.graph.command.application

import com.docgraph.backend.graph.command.domain.DependencyEdge
import com.docgraph.backend.graph.command.domain.DependencyEdgeRepository
import com.docgraph.backend.graph.command.domain.ValidationPairCreatedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class CreateDependencyEdgeCommandHandler(
    private val edgeRepository: DependencyEdgeRepository,
    private val publisher: ApplicationEventPublisher,
) {
    @Transactional
    fun handle(command: CreateDependencyEdgeCommand): DependencyEdge {
        edgeRepository.findByProjectIdAndSourceDocumentIdAndTargetDocumentId(
            projectId = command.projectId,
            sourceDocumentId = command.sourceDocumentId,
            targetDocumentId = command.targetDocumentId,
        )?.let { return it }

        val now = OffsetDateTime.now()
        val edge = edgeRepository.save(
            DependencyEdge(
                projectId = command.projectId,
                sourceDocumentId = command.sourceDocumentId,
                targetDocumentId = command.targetDocumentId,
                ruleId = command.ruleId,
                validationCriterion = command.validationCriterion,
                source = command.source,
                createdAt = now,
                updatedAt = now,
            ),
        )

        publisher.publishEvent(
            ValidationPairCreatedEvent(
                validationPairId = command.validationPairId,
                edgeId = edge.id,
                occurredAt = now,
            ),
        )

        return edge
    }
}
