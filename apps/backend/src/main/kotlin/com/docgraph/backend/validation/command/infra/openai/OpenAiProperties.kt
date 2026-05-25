package com.docgraph.backend.validation.command.infra.openai

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("ai.openai")
data class OpenAiProperties(
    val apiKey: String,
    val model: String,
    val baseUrl: String,
    val timeoutMs: Long,
    val chatCompletionsPath: String = "/v1/chat/completions",
    val responseFormatEnabled: Boolean = true,
)
