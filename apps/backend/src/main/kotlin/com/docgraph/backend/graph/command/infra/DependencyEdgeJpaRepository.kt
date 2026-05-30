package com.docgraph.backend.graph.command.infra

import com.docgraph.backend.graph.command.domain.DependencyEdge
import org.springframework.data.jpa.repository.JpaRepository

interface DependencyEdgeJpaRepository : JpaRepository<DependencyEdge, Long> {
    fun findByProjectIdAndId(projectId: Long, id: Long): DependencyEdge?
    fun findByProjectIdAndSourceDocumentIdAndTargetDocumentId(
        projectId: Long,
        sourceDocumentId: Long,
        targetDocumentId: Long,
    ): DependencyEdge?
    fun findAllByProjectId(projectId: Long): List<DependencyEdge>
    fun findAllByProjectIdAndTargetDocumentIdIn(
        projectId: Long,
        targetDocumentIds: List<Long>,
    ): List<DependencyEdge>
    fun findAllBySourceDocumentId(sourceDocumentId: Long): List<DependencyEdge>
    fun deleteAllByProjectId(projectId: Long)
}
