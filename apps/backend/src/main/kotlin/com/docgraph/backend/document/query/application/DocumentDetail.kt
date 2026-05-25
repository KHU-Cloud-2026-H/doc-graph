package com.docgraph.backend.document.query.application

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

data class DocumentDetail(
    @Schema(description = "문서 ID", example = "1")
    val id: Long,
    @Schema(description = "Notion 페이지 ID", example = "abc1234567890def")
    val notionPageId: String,
    @Schema(description = "문서 제목", example = "2024-01 스프린트 회의록")
    val title: String,
    @Schema(description = "문서 타입 (project type-mapping 미분류 시 null)")
    val type: DocumentType?,
    @Schema(description = "부모 Document ID (루트 또는 프로젝트 경계 밖이면 null)", example = "1")
    val parentDocumentId: Long?,
    @Schema(description = "Notion page 아이콘 (없으면 null)")
    val icon: IconResponse?,
    @Schema(description = "담당자 워크스페이스 멤버 ID (없으면 null)", example = "1")
    val assigneeMemberId: Long?,
    @Schema(description = "Notion 원본의 마지막 수정 시각 (미동기화 시 null)")
    val notionLastEditedAt: OffsetDateTime?,
    @Schema(description = "블록 row 목록")
    val blocks: List<Block>,
)