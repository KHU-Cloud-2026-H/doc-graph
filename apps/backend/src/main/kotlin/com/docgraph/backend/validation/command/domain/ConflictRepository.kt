package com.docgraph.backend.validation.command.domain

import org.springframework.data.jpa.repository.JpaRepository

interface ConflictRepository : JpaRepository<Conflict, Long> {
    fun findFirstByEdgeIdAndResolvedAtIsNull(edgeId: Long): Conflict?
}