package com.docgraph.backend.graph.command.application

import com.docgraph.backend.graph.command.domain.GraphRule
import com.docgraph.backend.graph.command.domain.GraphRuleDuplicateException
import com.docgraph.backend.graph.command.domain.GraphRuleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegisterGraphRuleCommandHandler(
    private val ruleRepository: GraphRuleRepository,
) {
    @Transactional
    fun handle(command: RegisterGraphRuleCommand): Long {
        val duplicates = ruleRepository.findAllByProjectIdAndTypePair(
            projectId = command.projectId,
            sourceType = command.sourceType,
            targetType = command.targetType,
        )
        if (duplicates.isNotEmpty()) {
            throw GraphRuleDuplicateException(command.sourceType, command.targetType)
        }
        val rule = GraphRule(
            projectId = command.projectId,
            sourceType = command.sourceType,
            targetType = command.targetType,
            validationCriterion = command.validationCriterion,
            isDefault = false,
        )
        return ruleRepository.save(rule).id
    }
}
