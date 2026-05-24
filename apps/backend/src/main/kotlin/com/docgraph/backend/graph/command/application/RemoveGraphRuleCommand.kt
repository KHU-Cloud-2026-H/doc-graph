package com.docgraph.backend.graph.command.application

data class RemoveGraphRuleCommand(
    val projectId: Long,
    val ruleId: Long,
)
