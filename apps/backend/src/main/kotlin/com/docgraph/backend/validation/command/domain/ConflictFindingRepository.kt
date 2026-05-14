package com.docgraph.backend.validation.command.domain

import org.springframework.data.jpa.repository.JpaRepository

interface ConflictFindingRepository : JpaRepository<ConflictFinding, Long> {
    fun findByConflictIdOrderByDetectedAtDesc(conflictId: Long): List<ConflictFinding>
}