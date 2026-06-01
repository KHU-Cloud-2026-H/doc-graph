package com.docgraph.backend.validation.command.infra

import com.docgraph.backend.validation.command.domain.AiUsageRecord
import org.springframework.data.jpa.repository.JpaRepository

interface AiUsageRecordJpaRepository : JpaRepository<AiUsageRecord, Long> {
    fun findByValidationTaskId(validationTaskId: Long): AiUsageRecord?
}
