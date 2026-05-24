package com.docgraph.backend.graph.command.domain

interface DependencyEdgeRepository {
    fun save(edge: DependencyEdge): DependencyEdge
    fun findById(id: Long): DependencyEdge?
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
    fun findAllByIds(ids: List<Long>): List<DependencyEdge>
    fun delete(edge: DependencyEdge)
    fun deleteAllByProjectId(projectId: Long)
}
