package com.docgraph.backend.graph.query.application

import com.docgraph.backend.document.query.application.DocumentType
import io.swagger.v3.oas.annotations.media.Schema

data class RuleResponse(
    @Schema(description = "룰 ID", example = "1")
    val id: Long,
    @Schema(description = "소속 프로젝트 ID (기본 제공 룰은 null)", example = "1")
    val projectId: Long?,
    @Schema(description = "출발 문서 타입")
    val sourceType: DocumentType,
    @Schema(description = "도착 문서 타입")
    val targetType: DocumentType,
    @Schema(description = "검증 기준", example = "범위 일치 여부")
    val validationCriterion: String,
    @Schema(description = "기본 제공 룰 여부", example = "false")
    val isDefault: Boolean,
)
