package com.docgraph.backend.graph.command.application

import java.util.UUID

data class AcceptEdgeProposalCommand(
    val projectId: Long,
    val proposalId: Long,
    val validationPairId: UUID = UUID.randomUUID(),
)
