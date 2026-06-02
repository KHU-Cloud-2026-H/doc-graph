package com.docgraph.backend.validation.command.application

import com.docgraph.backend.validation.command.domain.AiUsage

data class RecordAiUsageCommand(
    val validationTaskId: Long,
    val projectId: Long,
    val usage: AiUsage,
)
