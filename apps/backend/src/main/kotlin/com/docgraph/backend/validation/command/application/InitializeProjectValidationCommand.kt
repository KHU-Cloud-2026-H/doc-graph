package com.docgraph.backend.validation.command.application

data class InitializeProjectValidationCommand(
    val projectId: Long,
    val enabled: Boolean,
)
