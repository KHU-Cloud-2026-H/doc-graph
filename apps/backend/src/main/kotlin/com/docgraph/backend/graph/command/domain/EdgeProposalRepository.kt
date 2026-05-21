package com.docgraph.backend.graph.command.domain

interface EdgeProposalRepository {
    fun save(proposal: EdgeProposal): EdgeProposal
    fun findByProjectIdAndId(projectId: Long, id: Long): EdgeProposal?
    fun findByProjectIdAndSourceDocumentIdAndTargetDocumentId(
        projectId: Long,
        sourceDocumentId: Long,
        targetDocumentId: Long,
    ): EdgeProposal?
    fun findAllByProjectId(projectId: Long): List<EdgeProposal>
    fun delete(proposal: EdgeProposal)
    fun deleteAllByProjectId(projectId: Long)
}
