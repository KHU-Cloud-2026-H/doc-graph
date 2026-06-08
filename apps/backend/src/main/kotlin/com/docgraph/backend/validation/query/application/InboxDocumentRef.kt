package com.docgraph.backend.validation.query.application

import com.docgraph.backend.document.query.application.DocumentType
import io.swagger.v3.oas.annotations.media.Schema

data class InboxDocumentRef(
    @Schema(description = "문서 ID", example = "1")
    val id: Long,
    @Schema(description = "문서 제목", example = "PRD")
    val title: String,
    @Schema(description = "문서 타입 (미분류 시 null)")
    val type: DocumentType?,
)
