package com.docgraph.backend.graph.query.application

import com.docgraph.backend.graph.command.domain.GraphRule
import com.docgraph.backend.graph.command.domain.GraphRuleRepository
import org.springframework.stereotype.Service

@Service
class SearchGraphRulesByProjectQueryHandler(
    private val ruleRepository: GraphRuleRepository,
) : SearchGraphRulesByProjectQuery {
    override fun search(projectId: Long): List<RuleResponse> =
        ruleRepository.findAllApplicableToProject(projectId).map { it.toResponse() }
}

fun GraphRule.toResponse(): RuleResponse =
    RuleResponse(
        id = id,
        projectId = projectId,
        sourceType = sourceType,
        targetType = targetType,
        validationCriterion = validationCriterion,
        isDefault = isDefault,
    )
