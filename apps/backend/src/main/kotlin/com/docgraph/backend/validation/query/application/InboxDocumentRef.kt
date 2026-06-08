package com.docgraph.backend.validation.query.application

import com.docgraph.backend.document.query.application.DocumentType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

data class InboxDocumentRef(
    @Schema(description = "문서 ID", example = "1")
    val id: Long,
    @Schema(description = "문서 제목", example = "PRD")
    val title: String,
    @Schema(description = "문서 타입 (미분류 시 null)")
    val type: DocumentType?,
    @Schema(description = "Notion 최종 수정 시각. 수정 제안 승인 시 stale 가드용으로 client가 그대로 echo. 미동기화 시 null.")
    val notionLastEditedAt: OffsetDateTime?,
)
