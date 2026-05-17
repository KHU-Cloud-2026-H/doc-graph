package com.docgraph.backend.validation.command.infra

import com.docgraph.backend.validation.command.domain.ValidationTask
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.OffsetDateTime
import java.util.UUID

interface ValidationTaskJpaRepository : JpaRepository<ValidationTask, Long> {

    @Query(
        """
        SELECT t FROM ValidationTask t
        WHERE t.status = 'PENDING'
          AND (t.lastAttemptAt IS NULL OR t.lastAttemptAt < :before)
        """,
    )
    fun findStale(before: OffsetDateTime): List<ValidationTask>

    fun findByValidationPairId(validationPairId: UUID): ValidationTask?
}
