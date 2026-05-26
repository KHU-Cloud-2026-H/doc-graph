package com.docgraph.backend.document.command.infra.notion

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("notion.api")
data class NotionDocumentProperties(
    val baseUrl: String,
    val apiKey: String = "",
)
