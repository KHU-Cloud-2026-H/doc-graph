package com.docgraph.backend.project.query.application

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

data class ProjectDetail(
    @Schema(description = "프로젝트 ID", example = "1")
    val id: Long,
    @Schema(description = "프로젝트 이름", example = "Q2 Planning")
    val name: String,
    @Schema(description = "Notion 루트 페이지 ID", example = "abc1234567890def")
    val notionRootPageId: String,
    val members: List<ProjectMemberSummary>,
    @Schema(description = "프로젝트에 배정된 멤버 수", example = "5")
    val memberCount: Int,
    @Schema(description = "동기화된 문서 수", example = "42")
    val documentCount: Long,
    @Schema(description = "미해소(active 미무시) 충돌 수", example = "3")
    val unresolvedConflictCount: Long,
    @Schema(description = "프로젝트 내 문서의 Notion 측 최근 변경 시각", nullable = true)
    val lastNotionChangedAt: OffsetDateTime?,
)
