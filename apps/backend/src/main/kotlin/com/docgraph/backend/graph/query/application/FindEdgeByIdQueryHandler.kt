package com.docgraph.backend.graph.query.application

import com.docgraph.backend.graph.command.domain.DependencyEdge
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Service

@Service
class FindEdgeByIdQueryHandler(
    @PersistenceContext private val em: EntityManager,
) : FindEdgeByIdQuery {
    override fun find(edgeId: Long): EdgeDetail? {
        val edge = em.find(DependencyEdge::class.java, edgeId) ?: return null
        return EdgeDetail(
            id = edge.id,
            sourceDocumentId = edge.sourceDocumentId,
            targetDocumentId = edge.targetDocumentId,
            validationCriterion = edge.validationCriterion,
        )
    }
}
