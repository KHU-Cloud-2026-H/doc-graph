package com.docgraph.backend.graph.command.infra

import com.docgraph.backend.document.query.application.DocumentType
import com.docgraph.backend.graph.command.domain.GraphRule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface GraphRuleJpaRepository : JpaRepository<GraphRule, Long> {
    fun findByProjectIdAndId(projectId: Long, id: Long): GraphRule?

    @Query(
        """
        SELECT r FROM GraphRule r
        WHERE r.isDefault = true OR r.projectId = :projectId
        """,
    )
    fun findAllApplicableToProject(projectId: Long): List<GraphRule>

    @Query(
        """
        SELECT r FROM GraphRule r
        WHERE (r.isDefault = true OR r.projectId = :projectId)
          AND r.sourceType = :sourceType
          AND r.targetType = :targetType
        """,
    )
    fun findAllApplicableByTypePair(
        projectId: Long,
        sourceType: DocumentType,
        targetType: DocumentType,
    ): List<GraphRule>

    fun deleteAllByProjectId(projectId: Long)
}
