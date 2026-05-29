package com.docgraph.backend.project.query.application

import io.swagger.v3.oas.annotations.media.Schema

data class ProjectSummary(
    @Schema(description = "프로젝트 ID", example = "1")
    val id: Long,
    @Schema(description = "프로젝트 이름", example = "Q2 Planning")
    val name: String,
    @Schema(description = "소속 워크스페이스 ID", example = "1")
    val workspaceId: Long,
)
