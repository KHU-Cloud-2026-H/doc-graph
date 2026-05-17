package com.docgraph.backend.workspace.query.application

import io.swagger.v3.oas.annotations.media.Schema

data class WorkspaceDetail(
    @Schema(description = "워크스페이스 ID", example = "1")
    val id: Long,
    @Schema(description = "워크스페이스 이름", example = "My Team")
    val name: String,
    val members: List<WorkspaceMemberSummary>,
)
