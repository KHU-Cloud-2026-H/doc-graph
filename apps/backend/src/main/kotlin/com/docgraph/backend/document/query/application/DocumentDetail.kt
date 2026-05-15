package com.docgraph.backend.document.query.application

import io.swagger.v3.oas.annotations.media.Schema

data class DocumentDetail(
    @Schema(description = "문서 ID", example = "1")
    val id: Long,
    @Schema(description = "Notion 페이지 ID", example = "abc1234567890def")
    val notionPageId: String,
    @Schema(description = "문서 제목", example = "2024-01 스프린트 회의록")
    val title: String,
    @Schema(description = "문서 타입 (project type-mapping 미분류 시 null)")
    val type: DocumentType?,
    @Schema(description = "담당자 워크스페이스 멤버 ID (없으면 null)", example = "1")
    val assigneeMemberId: Long?,
    @Schema(description = "블록 row 목록")
    val blocks: List<Block>,
)