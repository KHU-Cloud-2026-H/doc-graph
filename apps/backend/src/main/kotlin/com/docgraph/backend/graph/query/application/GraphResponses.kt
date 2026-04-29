package com.docgraph.backend.graph.query.application

import com.docgraph.backend.common.ConflictStatus
import com.docgraph.backend.common.DocumentType
import io.swagger.v3.oas.annotations.media.Schema

data class ProjectGraphResponse(
    val nodes: List<GraphNodeResponse>,
    val edges: List<EdgeResponse>,
    val proposals: List<EdgeProposalResponse>,
)

data class GraphNodeResponse(
    @Schema(description = "문서 ID", example = "1")
    val id: Long,
    @Schema(description = "문서 제목", example = "2024-01 스프린트 회의록")
    val title: String,
    @Schema(description = "문서 타입")
    val type: DocumentType,
    @Schema(description = "담당자 워크스페이스 멤버 ID (없으면 null)", example = "1")
    val assigneeMemberId: Long?,
)

data class EdgeResponse(
    @Schema(description = "엣지 ID", example = "1")
    val id: Long,
    @Schema(description = "출발 문서 ID", example = "1")
    val sourceDocumentId: Long,
    @Schema(description = "도착 문서 ID", example = "2")
    val targetDocumentId: Long,
    @Schema(description = "체크 항목", example = "범위 일치 여부")
    val checkItem: String,
    @Schema(description = "충돌 상태")
    val conflictStatus: ConflictStatus,
)

data class EdgeProposalResponse(
    @Schema(description = "연결 제안 ID", example = "1")
    val id: Long,
    @Schema(description = "출발 문서 ID", example = "1")
    val sourceDocumentId: Long,
    @Schema(description = "도착 문서 ID", example = "2")
    val targetDocumentId: Long,
    @Schema(description = "유사도 점수 (0.0 ~ 1.0)", example = "0.87")
    val similarityScore: Double,
)

data class RuleResponse(
    @Schema(description = "룰 ID", example = "1")
    val id: Long,
    @Schema(description = "출발 문서 타입")
    val sourceType: DocumentType,
    @Schema(description = "도착 문서 타입")
    val targetType: DocumentType,
    @Schema(description = "체크 항목", example = "범위 일치 여부")
    val checkItem: String,
    @Schema(description = "기본 제공 룰 여부", example = "false")
    val isDefault: Boolean,
)
