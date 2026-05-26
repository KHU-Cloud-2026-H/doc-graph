package com.docgraph.backend.workspace.query.application

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

data class NotionWorkspacePageResponse(
    @Schema(description = "Notion page ID", example = "abc1234567890def")
    val id: String,
    @Schema(description = "Notion page title", example = "Product Requirements")
    val title: String,
    @Schema(description = "Notion page URL")
    val url: String?,
    @Schema(description = "Notion 원본의 마지막 수정 시각")
    val lastEditedTime: OffsetDateTime?,
)
