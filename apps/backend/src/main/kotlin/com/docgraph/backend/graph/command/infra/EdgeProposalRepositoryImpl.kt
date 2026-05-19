package com.docgraph.backend.graph.command.infra

import com.docgraph.backend.graph.command.domain.EdgeProposal
import com.docgraph.backend.graph.command.domain.EdgeProposalRepository
import org.springframework.stereotype.Component

@Component
class EdgeProposalRepositoryImpl(
    private val jpa: EdgeProposalJpaRepository,
) : EdgeProposalRepository {
    override fun save(proposal: EdgeProposal): EdgeProposal = jpa.save(proposal)
    override fun findByProjectIdAndId(projectId: Long, id: Long): EdgeProposal? =
        jpa.findByProjectIdAndId(projectId, id)
    override fun findByProjectIdAndSourceDocumentIdAndTargetDocumentId(
        projectId: Long,
        sourceDocumentId: Long,
        targetDocumentId: Long,
    ): EdgeProposal? =
        jpa.findByProjectIdAndSourceDocumentIdAndTargetDocumentId(projectId, sourceDocumentId, targetDocumentId)
    override fun findAllByProjectId(projectId: Long): List<EdgeProposal> = jpa.findAllByProjectId(projectId)
    override fun delete(proposal: EdgeProposal) = jpa.delete(proposal)
    override fun deleteAllByProjectId(projectId: Long) = jpa.deleteAllByProjectId(projectId)
}
