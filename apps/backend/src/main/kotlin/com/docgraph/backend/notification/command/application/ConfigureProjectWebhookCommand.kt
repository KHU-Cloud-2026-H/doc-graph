package com.docgraph.backend.notification.command.application

data class ConfigureProjectWebhookCommand(
    val projectId: Long,
    val requesterUserId: Long,
    val url: String,
)
