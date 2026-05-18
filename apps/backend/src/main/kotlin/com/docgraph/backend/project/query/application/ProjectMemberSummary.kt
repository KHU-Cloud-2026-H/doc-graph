package com.docgraph.backend.project.query.application

import com.docgraph.backend.project.command.domain.ProjectMemberRole
import io.swagger.v3.oas.annotations.media.Schema

data class ProjectMemberSummary(
    @Schema(description = "멤버 ID", example = "1")
    val id: Long,
    @Schema(description = "이름", example = "홍길동")
    val name: String,
    @Schema(description = "역할")
    val role: ProjectMemberRole,
)
