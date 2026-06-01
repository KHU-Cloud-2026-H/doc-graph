package com.docgraph.backend.document.command.infra.notion

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties("notion.webhook")
data class NotionWebhookProperties(
    val secret: String = "",
)

@Configuration
@EnableConfigurationProperties(NotionWebhookProperties::class)
class NotionWebhookConfig
