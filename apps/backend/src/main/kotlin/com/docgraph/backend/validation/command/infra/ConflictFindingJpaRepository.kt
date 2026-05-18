package com.docgraph.backend.validation.command.infra

import com.docgraph.backend.validation.command.domain.ConflictFinding
import org.springframework.data.jpa.repository.JpaRepository

interface ConflictFindingJpaRepository : JpaRepository<ConflictFinding, Long> {
    fun findByConflictIdOrderByDetectedAtDesc(conflictId: Long): List<ConflictFinding>
}
