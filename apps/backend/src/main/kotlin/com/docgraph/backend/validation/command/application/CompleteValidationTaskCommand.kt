package com.docgraph.backend.validation.command.application

import com.docgraph.backend.validation.command.domain.DetectedConflict

data class CompleteValidationTaskCommand(
    val validationTaskId: Long,
    val findings: List<DetectedConflict>,
)
