package com.docgraph.backend.validation.command.application

data class ConfigureProjectValidationCommand(
    val projectId: Long,
    val requesterUserId: Long,
    val enabled: Boolean,
)
