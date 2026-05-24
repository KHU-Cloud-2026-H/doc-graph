package com.docgraph.backend.workspace.query.application

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

data class WorkspaceSummary(
    @Schema(description = "워크스페이스 ID", example = "1")
    val id: Long,
    @Schema(description = "워크스페이스 이름", example = "My Team")
    val name: String,
    @Schema(description = "워크스페이스에 직접 초대된 멤버 수", example = "7")
    val memberCount: Int,
    @Schema(description = "워크스페이스 내 프로젝트 수", example = "3")
    val projectCount: Int,
    @Schema(description = "워크스페이스 내 모든 프로젝트의 동기화된 문서 합산", example = "120")
    val documentCount: Long,
    @Schema(description = "워크스페이스 내 모든 프로젝트의 미해소(active 미무시) 충돌 합산", example = "5")
    val unresolvedConflictCount: Long,
    @Schema(description = "워크스페이스 내 모든 프로젝트 중 Notion 측 최근 변경 시각 max", nullable = true)
    val lastNotionChangedAt: OffsetDateTime?,
)
