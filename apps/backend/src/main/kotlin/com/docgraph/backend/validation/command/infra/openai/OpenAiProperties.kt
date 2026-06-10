package com.docgraph.backend.validation.command.infra.openai

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("ai.openai")
data class OpenAiProperties(
    val apiKey: String,
    val model: String,
    val baseUrl: String,
    // TCP 연결 수립 한도. 짧게 둔다 — 연결이 안 잡히면 빠르게 실패시켜 재시도로 넘긴다.
    val connectTimeoutMs: Long,
    // 요청 전송~본문 완수신 한도. LLM 생성 지연을 흡수하도록 길게 둔다.
    val readTimeoutMs: Long,
)
