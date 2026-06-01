package com.docgraph.backend.validation.query.application

import com.docgraph.backend.validation.command.domain.FailureCategory
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

data class ValidationTaskResponse(
    @Schema(description = "검증 작업 ID", example = "1")
    val id: Long,
    @Schema(description = "검증 대상 엣지 ID", example = "1")
    val edgeId: Long,
    @Schema(description = "검증 상태")
    val status: ValidationStatus,
    @Schema(description = "실패 원인 분류 (status=FAILED일 때만 존재)", nullable = true)
    val failureCategory: FailureCategory?,
    @Schema(description = "생성 시각")
    val createdAt: OffsetDateTime,
)