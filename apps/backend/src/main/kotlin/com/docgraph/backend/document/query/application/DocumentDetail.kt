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
    @Schema(
        description = "최근 수정자 이름. 수정자가 본 서비스에 로그인한 적 있는 사용자일 때만 채워지며, " +
            "외부 Notion 협업자·봇이거나 미동기화면 null. 마지막 로그인 시점의 Notion 표시 이름 스냅샷.",
        example = "홍길동",
    )
    val lastEditedByName: String?,
    @Schema(description = "저장된 block 텍스트를 합친 값")
    val flatText: String?,
    @Schema(description = "블록 row 목록")
    val blocks: List<Block>,
)
