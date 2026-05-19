package com.docgraph.backend.graph.command.domain

import com.docgraph.backend.document.query.application.DocumentType

interface GraphRuleRepository {
    fun save(rule: GraphRule): GraphRule
    fun findByProjectIdAndId(projectId: Long, id: Long): GraphRule?
    fun findAllApplicableToProject(projectId: Long): List<GraphRule>
    fun findAllByProjectIdAndTypePair(
        projectId: Long,
        sourceType: DocumentType,
        targetType: DocumentType,
    ): List<GraphRule>
    fun delete(rule: GraphRule)
    fun deleteAllByProjectId(projectId: Long)
}
