package com.docgraph.backend.graph.query.application

import com.docgraph.backend.graph.command.domain.DependencyEdgeSource
import com.docgraph.backend.validation.query.application.ConflictStatus
import io.swagger.v3.oas.annotations.media.Schema

data class EdgeResponse(
    @Schema(description = "엣지 ID", example = "1")
    val id: Long,
    @Schema(description = "출발 문서 ID", example = "1")
    val sourceDocumentId: Long,
    @Schema(description = "도착 문서 ID", example = "2")
    val targetDocumentId: Long,
    @Schema(description = "검증 기준", example = "범위 일치 여부")
    val validationCriterion: String,
    @Schema(description = "충돌 상태")
    val conflictStatus: ConflictStatus,
    @Schema(description = "엣지 생성 출처 (Notion 링크/멘션 기반, 제안 수락, 수동 추가)")
    val source: DependencyEdgeSource,
    @Schema(description = "엣지 생성에 사용된 룰 ID (수동 추가 엣지는 null)", example = "1")
    val ruleId: Long?,
)
