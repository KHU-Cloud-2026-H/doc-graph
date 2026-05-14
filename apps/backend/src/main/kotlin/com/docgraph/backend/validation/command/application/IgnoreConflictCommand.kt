package com.docgraph.backend.validation.command.application

data class IgnoreConflictCommand(
    val conflictId: Long,
    val ignoredBy: Long,
    val reason: String?,
)