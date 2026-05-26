package com.docgraph.backend.document.query.application

import io.swagger.v3.oas.annotations.media.Schema

data class NotionPageRef(
    @Schema(description = "Notion 페이지 ID — 프로젝트 생성·카테고리 등록 API 입력에 사용", example = "abc1234567890def")
    val notionPageId: String,
    @Schema(description = "페이지 제목 (드롭다운·카테고리 화면 표시용)", example = "Q2 Planning")
    val title: String,
    @Schema(description = "Notion page 아이콘 (없으면 null)")
    val icon: IconResponse?,
)
