package com.docgraph.backend.common

import io.swagger.v3.oas.annotations.media.Schema

data class PageResponse<T>(
    val content: List<T>,
    @Schema(description = "전체 항목 수", example = "100")
    val totalElements: Long,
    @Schema(description = "전체 페이지 수", example = "5")
    val totalPages: Int,
    @Schema(description = "현재 페이지 (0-based)", example = "0")
    val page: Int,
    @Schema(description = "페이지 크기", example = "20")
    val size: Int,
)
