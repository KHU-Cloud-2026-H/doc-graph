package com.docgraph.backend.project.query.application

import com.docgraph.backend.document.query.application.DocumentType
import io.swagger.v3.oas.annotations.media.Schema

data class CategoryResponse(
    @Schema(description = "카테고리 ID", example = "1")
    val id: Long,
    @Schema(description = "Notion 페이지 ID", example = "abc1234567890def")
    val notionPageId: String,
    @Schema(description = "문서 타입")
    val documentType: DocumentType,
)
