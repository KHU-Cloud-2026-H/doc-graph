package com.docgraph.backend.graph.command.infra

import com.docgraph.backend.graph.command.domain.EdgeProposal
import org.springframework.data.jpa.repository.JpaRepository

interface EdgeProposalJpaRepository : JpaRepository<EdgeProposal, Long> {
    fun findByProjectIdAndId(projectId: Long, id: Long): EdgeProposal?
    fun findByProjectIdAndSourceDocumentIdAndTargetDocumentId(
        projectId: Long,
        sourceDocumentId: Long,
        targetDocumentId: Long,
    ): EdgeProposal?
    fun findAllByProjectId(projectId: Long): List<EdgeProposal>
    fun deleteAllByProjectId(projectId: Long)
}
