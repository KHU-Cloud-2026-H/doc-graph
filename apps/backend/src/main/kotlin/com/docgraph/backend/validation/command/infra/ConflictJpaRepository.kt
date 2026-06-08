package com.docgraph.backend.validation.command.infra

import com.docgraph.backend.validation.command.domain.Conflict
import org.springframework.data.jpa.repository.JpaRepository

interface ConflictJpaRepository : JpaRepository<Conflict, Long> {
    fun findFirstByEdgeIdAndResolvedAtIsNull(edgeId: Long): Conflict?

    fun deleteByProjectId(projectId: Long)
}
