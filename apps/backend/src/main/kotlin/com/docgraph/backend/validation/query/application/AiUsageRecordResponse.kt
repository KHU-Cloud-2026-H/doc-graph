package com.docgraph.backend.validation.query.application

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

data class AiUsageRecordResponse(
    @Schema(description = "사용량을 발생시킨 검증 작업 ID", example = "1")
    val validationTaskId: Long,
    @Schema(description = "응답이 알려준 실제 model 버전", example = "gpt-4o-2024-08-06")
    val model: String,
    @Schema(description = "prompt 토큰", example = "1200")
    val promptTokens: Int,
    @Schema(description = "completion 토큰", example = "300")
    val completionTokens: Int,
    @Schema(description = "전체 토큰", example = "1500")
    val totalTokens: Int,
    @Schema(description = "호출 시각")
    val createdAt: OffsetDateTime,
)
