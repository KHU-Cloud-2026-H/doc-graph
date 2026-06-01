package com.docgraph.backend.validation.command.application

import com.docgraph.backend.validation.command.domain.AiUsageRecord
import com.docgraph.backend.validation.command.domain.AiUsageRecordRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class RecordAiUsageCommandHandler(
    private val repository: AiUsageRecordRepository,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(command: RecordAiUsageCommand) {
        // 태스크당 1행. 재호출 시 UNIQUE 위반 대신 idempotent skip.
        if (repository.findByValidationTaskId(command.validationTaskId) != null) return
        repository.save(
            AiUsageRecord(
                validationTaskId = command.validationTaskId,
                projectId = command.projectId,
                model = command.usage.model,
                promptTokens = command.usage.promptTokens,
                completionTokens = command.usage.completionTokens,
                totalTokens = command.usage.totalTokens,
            ),
        )
    }
}
