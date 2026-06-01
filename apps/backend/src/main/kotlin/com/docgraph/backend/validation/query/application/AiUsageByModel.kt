package com.docgraph.backend.validation.query.application

import io.swagger.v3.oas.annotations.media.Schema

data class AiUsageByModel(
    @Schema(description = "응답이 알려준 실제 model 버전", example = "gpt-4o-2024-08-06")
    val model: String,
    @Schema(description = "호출 수", example = "12")
    val calls: Long,
    @Schema(description = "prompt 토큰 합계", example = "14000")
    val promptTokens: Long,
    @Schema(description = "completion 토큰 합계", example = "3200")
    val completionTokens: Long,
    @Schema(description = "전체 토큰 합계", example = "17200")
    val totalTokens: Long,
)
