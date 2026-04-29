package com.docgraph.backend.project.query.application


import com.docgraph.backend.common.DocumentType
import com.docgraph.backend.common.ProjectMemberRole
import io.swagger.v3.oas.annotations.media.Schema

data class ProjectSummary(
    @Schema(description = "프로젝트 ID", example = "1")
    val id: Long,
    @Schema(description = "프로젝트 이름", example = "Q2 Planning")
    val name: String,
)

data class ProjectDetail(
    @Schema(description = "프로젝트 ID", example = "1")
    val id: Long,
    @Schema(description = "프로젝트 이름", example = "Q2 Planning")
    val name: String,
    @Schema(description = "Notion 루트 페이지 ID", example = "abc1234567890def")
    val notionRootPageId: String,
    val members: List<ProjectMemberSummary>,
)

data class ProjectMemberSummary(
    @Schema(description = "멤버 ID", example = "1")
    val id: Long,
    @Schema(description = "이름", example = "홍길동")
    val name: String,
    @Schema(description = "역할")
    val role: ProjectMemberRole,
)

data class TypeMappingResponse(
    @Schema(description = "Notion 페이지 ID", example = "abc1234567890def")
    val notionPageId: String,
    @Schema(description = "Notion 페이지 제목", example = "회의록")
    val notionPageTitle: String,
    @Schema(description = "문서 타입")
    val documentType: DocumentType,
)

data class TypeAssigneeResponse(
    @Schema(description = "문서 타입")
    val documentType: DocumentType,
    @Schema(description = "담당자 워크스페이스 멤버 ID (null이면 담당자 없음)", example = "1")
    val assigneeMemberId: Long?,
)
