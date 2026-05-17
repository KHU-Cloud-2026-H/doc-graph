package com.docgraph.backend.validation.command.infra

import com.docgraph.backend.validation.command.domain.ValidationTask
import com.docgraph.backend.validation.command.domain.ValidationTaskRepository
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.util.UUID

@Component
class ValidationTaskRepositoryImpl(
    private val jpa: ValidationTaskJpaRepository,
) : ValidationTaskRepository {
    override fun save(task: ValidationTask): ValidationTask = jpa.save(task)
    override fun findById(id: Long): ValidationTask? = jpa.findById(id).orElse(null)
    override fun findByValidationPairId(validationPairId: UUID): ValidationTask? =
        jpa.findByValidationPairId(validationPairId)
    override fun findStale(before: OffsetDateTime): List<ValidationTask> = jpa.findStale(before)
}
