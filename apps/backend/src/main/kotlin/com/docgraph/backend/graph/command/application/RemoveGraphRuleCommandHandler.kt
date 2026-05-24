package com.docgraph.backend.graph.command.application

import com.docgraph.backend.graph.command.domain.GraphRuleNotFoundException
import com.docgraph.backend.graph.command.domain.GraphRuleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RemoveGraphRuleCommandHandler(
    private val ruleRepository: GraphRuleRepository,
) {
    @Transactional
    fun handle(command: RemoveGraphRuleCommand) {
        val rule = ruleRepository.findByProjectIdAndId(command.projectId, command.ruleId)
            ?: throw GraphRuleNotFoundException(command.ruleId)
        rule.ensureRemovable()
        ruleRepository.delete(rule)
    }
}
