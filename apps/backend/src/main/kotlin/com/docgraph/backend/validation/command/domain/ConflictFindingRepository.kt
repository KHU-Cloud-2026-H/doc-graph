package com.docgraph.backend.validation.command.domain

interface ConflictFindingRepository {
    fun save(finding: ConflictFinding): ConflictFinding
    fun findById(id: Long): ConflictFinding?
    fun findByConflictIdOrderByDetectedAtDesc(conflictId: Long): List<ConflictFinding>
}