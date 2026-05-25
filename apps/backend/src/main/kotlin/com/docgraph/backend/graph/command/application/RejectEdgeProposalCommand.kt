package com.docgraph.backend.graph.command.application

data class RejectEdgeProposalCommand(
    val projectId: Long,
    val proposalId: Long,
)
