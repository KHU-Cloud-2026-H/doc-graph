package com.docgraph.backend.graph.query.application

import io.swagger.v3.oas.annotations.media.Schema

data class EdgeProposalResponse(
    @Schema(description = "연결 제안 ID", example = "1")
    val id: Long,
    @Schema(description = "출발 문서 ID", example = "1")
    val sourceDocumentId: Long,
    @Schema(description = "도착 문서 ID", example = "2")
    val targetDocumentId: Long,
    @Schema(description = "유사도 점수 (0.0 ~ 1.0)", example = "0.87")
    val similarityScore: Double,
    @Schema(description = "수락 시 적용될 검증 기준 (룰 기반 자동 매칭)", example = "범위 일치 여부")
    val validationCriterion: String,
)
