package com.docgraph.backend.graph.command.infra

import com.docgraph.backend.graph.command.domain.DependencyEdge
import com.docgraph.backend.graph.command.domain.DependencyEdgeRepository
import org.springframework.stereotype.Component

@Component
class DependencyEdgeRepositoryImpl(
    private val jpa: DependencyEdgeJpaRepository,
) : DependencyEdgeRepository {
    override fun save(edge: DependencyEdge): DependencyEdge = jpa.save(edge)
    override fun findById(id: Long): DependencyEdge? = jpa.findById(id).orElse(null)
    override fun findByProjectIdAndId(projectId: Long, id: Long): DependencyEdge? =
        jpa.findByProjectIdAndId(projectId, id)
    override fun findByProjectIdAndSourceDocumentIdAndTargetDocumentId(
        projectId: Long,
        sourceDocumentId: Long,
        targetDocumentId: Long,
    ): DependencyEdge? =
        jpa.findByProjectIdAndSourceDocumentIdAndTargetDocumentId(projectId, sourceDocumentId, targetDocumentId)
    override fun findAllByProjectId(projectId: Long): List<DependencyEdge> =
        jpa.findAllByProjectId(projectId)
    override fun findAllByTargetDocumentIdIn(targetDocumentIds: List<Long>): List<DependencyEdge> =
        if (targetDocumentIds.isEmpty()) emptyList() else jpa.findAllByTargetDocumentIdIn(targetDocumentIds)
    override fun delete(edge: DependencyEdge) = jpa.delete(edge)
    override fun deleteAllByProjectId(projectId: Long) = jpa.deleteAllByProjectId(projectId)
}
