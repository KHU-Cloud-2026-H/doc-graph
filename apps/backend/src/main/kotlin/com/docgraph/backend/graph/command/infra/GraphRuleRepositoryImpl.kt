package com.docgraph.backend.graph.command.infra

import com.docgraph.backend.document.query.application.DocumentType
import com.docgraph.backend.graph.command.domain.GraphRule
import com.docgraph.backend.graph.command.domain.GraphRuleRepository
import org.springframework.stereotype.Component

@Component
class GraphRuleRepositoryImpl(
    private val jpa: GraphRuleJpaRepository,
) : GraphRuleRepository {
    override fun save(rule: GraphRule): GraphRule = jpa.save(rule)
    override fun findByProjectIdAndId(projectId: Long, id: Long): GraphRule? =
        jpa.findByProjectIdAndId(projectId, id)
    override fun findAllApplicableToProject(projectId: Long): List<GraphRule> =
        jpa.findAllApplicableToProject(projectId)
    override fun findAllByProjectIdAndTypePair(
        projectId: Long,
        sourceType: DocumentType,
        targetType: DocumentType,
    ): List<GraphRule> =
        jpa.findAllApplicableByTypePair(projectId, sourceType, targetType)
    override fun delete(rule: GraphRule) = jpa.delete(rule)
    override fun deleteAllByProjectId(projectId: Long) = jpa.deleteAllByProjectId(projectId)
}
