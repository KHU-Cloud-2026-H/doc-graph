package com.docgraph.backend.validation.command.domain

import com.docgraph.backend.event.OutboxRepository
import java.time.OffsetDateTime
import java.util.UUID

interface ValidationTaskRepository : OutboxRepository<ValidationTask> {
    fun save(task: ValidationTask): ValidationTask
    fun findById(id: Long): ValidationTask?
    fun findByValidationPairId(validationPairId: UUID): ValidationTask?
    override fun findStale(before: OffsetDateTime): List<ValidationTask>
}