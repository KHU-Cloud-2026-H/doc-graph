package com.docgraph.backend.validation.command.domain

interface AiUsageRecordRepository {
    fun save(record: AiUsageRecord): AiUsageRecord
    fun findByValidationTaskId(validationTaskId: Long): AiUsageRecord?
}
