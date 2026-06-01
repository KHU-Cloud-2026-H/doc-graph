package com.docgraph.backend.validation.query.application

import io.swagger.v3.oas.annotations.media.Schema

data class AiUsageSummaryResponse(
    @Schema(description = "총 호출 수", example = "30")
    val totalCalls: Long,
    @Schema(description = "총 prompt 토큰", example = "36000")
    val totalPromptTokens: Long,
    @Schema(description = "총 completion 토큰", example = "8000")
    val totalCompletionTokens: Long,
    @Schema(description = "총 토큰", example = "44000")
    val totalTokens: Long,
    @Schema(description = "모델별 분해")
    val byModel: List<AiUsageByModel>,
)
