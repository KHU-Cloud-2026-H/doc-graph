package com.docgraph.backend.validation.command.infra

import com.docgraph.backend.validation.command.domain.ConflictFinding
import com.docgraph.backend.validation.command.domain.ConflictFindingRepository
import org.springframework.stereotype.Component

@Component
class ConflictFindingRepositoryImpl(
    private val jpa: ConflictFindingJpaRepository,
) : ConflictFindingRepository {
    override fun save(finding: ConflictFinding): ConflictFinding = jpa.save(finding)
    override fun findById(id: Long): ConflictFinding? = jpa.findById(id).orElse(null)
    override fun findByConflictIdOrderByDetectedAtDesc(conflictId: Long): List<ConflictFinding> =
        jpa.findByConflictIdOrderByDetectedAtDesc(conflictId)
}
