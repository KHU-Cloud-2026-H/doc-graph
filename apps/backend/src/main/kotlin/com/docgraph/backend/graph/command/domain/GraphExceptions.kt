package com.docgraph.backend.graph.command.domain

class DependencyEdgeNotFoundException(edgeId: Long) : RuntimeException(
    "dependency edge not found: $edgeId",
)

class EdgeProposalNotFoundException(proposalId: Long) : RuntimeException(
    "edge proposal not found: $proposalId",
)

class GraphRuleNotFoundException(ruleId: Long) : RuntimeException(
    "graph rule not found: $ruleId",
)

class DefaultGraphRuleCannotBeRemovedException(ruleId: Long) : RuntimeException(
    "default graph rule cannot be removed: $ruleId",
)

class GraphRuleDuplicateException(sourceType: Any, targetType: Any) : RuntimeException(
    "graph rule already exists for ($sourceType, $targetType)",
)
