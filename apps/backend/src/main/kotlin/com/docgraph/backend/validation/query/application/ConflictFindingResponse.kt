package com.docgraph.backend.validation.query.application

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

data class ConflictFindingResponse(
    @Schema(description = "충돌 finding ID", example = "1")
    val id: Long,
    @Schema(description = "source 측 충돌 블록 ID 목록 (Notion block id)")
    val sourceBlockIds: List<String>,
    @Schema(description = "target 측 충돌 블록 ID (Notion block id)")
    val targetBlockId: String,
    @Schema(description = "충돌 근거 (AI 판정)", example = "planning 3.2절 결정사항이 requirements에 반영되지 않음")
    val rationale: String,
    @Schema(description = "target 블록을 교체할 새 텍스트 (replace patch)", example = "requirements 3.2절을 'A 옵션 선택'으로 변경")
    val newText: String,
    @Schema(description = "충돌 사안 한 줄 요약 제목", example = "결정사항 미반영")
    val title: String,
    @Schema(description = "감지 시각")
    val detectedAt: OffsetDateTime,
)