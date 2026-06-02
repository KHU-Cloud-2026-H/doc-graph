package com.docgraph.backend.validation.command.domain

/** AI 호출 1회의 자원 사용량. model은 응답이 알려준 실제 버전(설정 alias가 아님). */
data class AiUsage(
    val model: String,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
)
