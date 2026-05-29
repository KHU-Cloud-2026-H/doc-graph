package com.docgraph.backend.notification.command.infra

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("notification.webhook")
data class WebhookNotifierProperties(
    val timeoutMs: Long,
)
