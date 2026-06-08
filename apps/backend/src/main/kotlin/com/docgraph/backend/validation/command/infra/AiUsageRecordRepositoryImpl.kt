package com.docgraph.backend.validation.command.infra

import com.docgraph.backend.validation.command.domain.AiUsageRecord
import com.docgraph.backend.validation.command.domain.AiUsageRecordRepository
import org.springframework.stereotype.Component

@Component
class AiUsageRecordRepositoryImpl(
    private val jpa: AiUsageRecordJpaRepository,
) : AiUsageRecordRepository {
    override fun save(record: AiUsageRecord): AiUsageRecord = jpa.save(record)
    override fun findByValidationTaskId(validationTaskId: Long): AiUsageRecord? =
        jpa.findByValidationTaskId(validationTaskId)
    override fun deleteByProjectId(projectId: Long) = jpa.deleteByProjectId(projectId)
}
