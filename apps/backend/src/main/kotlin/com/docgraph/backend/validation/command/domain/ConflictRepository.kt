package com.docgraph.backend.validation.command.domain

interface ConflictRepository {
    fun save(conflict: Conflict): Conflict
    fun findById(id: Long): Conflict?
    fun findFirstByEdgeIdAndResolvedAtIsNull(edgeId: Long): Conflict?
}