package com.docgraph.backend.validation.command.infra

import com.docgraph.backend.validation.command.domain.Conflict
import com.docgraph.backend.validation.command.domain.ConflictRepository
import org.springframework.stereotype.Component

@Component
class ConflictRepositoryImpl(
    private val jpa: ConflictJpaRepository,
) : ConflictRepository {
    override fun save(conflict: Conflict): Conflict = jpa.save(conflict)
    override fun findById(id: Long): Conflict? = jpa.findById(id).orElse(null)
    override fun findFirstByEdgeIdAndResolvedAtIsNull(edgeId: Long): Conflict? =
        jpa.findFirstByEdgeIdAndResolvedAtIsNull(edgeId)
    override fun deleteByProjectId(projectId: Long) = jpa.deleteByProjectId(projectId)
}
