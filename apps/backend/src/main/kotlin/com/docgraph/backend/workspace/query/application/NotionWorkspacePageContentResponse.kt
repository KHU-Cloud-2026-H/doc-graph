package com.docgraph.backend.workspace.query.application

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

data class NotionWorkspacePageContentResponse(
    @Schema(description = "Notion page ID")
    val id: String,
    @Schema(description = "Notion page title")
    val title: String,
    @Schema(description = "page 하위 block 텍스트를 순서대로 합친 값")
    val flatText: String?,
    @Schema(description = "Notion 원본의 마지막 수정 시각")
    val lastEditedTime: OffsetDateTime?,
    @Schema(description = "page 하위 block tree를 pre-order로 펼친 목록")
    val blocks: List<NotionWorkspaceBlockResponse>,
)

data class NotionWorkspaceBlockResponse(
    val id: String,
    val type: String,
    val parentId: String?,
    val text: String?,
    val childPageTitle: String?,
    val linkedPageIds: Set<String>,
    val hasChildren: Boolean,
    val archived: Boolean,
    val inTrash: Boolean,
    val sortOrder: Int,
)
