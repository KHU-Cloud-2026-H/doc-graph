package com.docgraph.backend.graph.query.application

fun interface SearchGraphRulesByProjectQuery {
    fun search(projectId: Long): List<RuleResponse>
}
