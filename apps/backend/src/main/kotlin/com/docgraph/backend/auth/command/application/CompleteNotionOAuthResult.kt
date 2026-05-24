package com.docgraph.backend.auth.command.application

data class CompleteNotionOAuthResult(
    val userId: Long,
    val sessionToken: String,
    val sessionExpiresInSeconds: Long,
)
