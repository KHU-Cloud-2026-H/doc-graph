package com.docgraph.backend.workspace.query.application

import com.docgraph.backend.document.command.domain.NotionIcon
import com.docgraph.backend.document.command.domain.NotionIconType
import com.docgraph.backend.document.query.application.IconResponse
import com.docgraph.backend.document.query.application.IconType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

data class NotionWorkspacePageResponse(
    @Schema(description = "Notion page ID", example = "abc1234567890def")
    val notionPageId: String,
    @Schema(description = "Notion page title", example = "Product Requirements")
    val title: String,
    @Schema(description = "Notion page icon")
    val icon: IconResponse?,
    @Schema(description = "Notion page URL")
    val url: String?,
    @Schema(description = "Notion 원본의 마지막 수정 시각")
    val lastEditedTime: OffsetDateTime?,
    @Schema(description = "기존 응답 호환용 alias. notionPageId와 동일")
    val id: String = notionPageId,
)

data class NotionWorkspacePageMetadataResponse(
    @Schema(description = "Notion page title", example = "Product Requirements")
    val title: String,
    @Schema(description = "Notion page icon")
    val icon: IconResponse?,
)

fun NotionIcon?.toIconResponse(): IconResponse? = when (this?.type) {
    NotionIconType.EMOJI -> IconResponse(IconType.EMOJI, value)
    NotionIconType.EXTERNAL -> IconResponse(IconType.EXTERNAL, value)
    NotionIconType.FILE -> IconResponse(IconType.FILE, value)
    NotionIconType.NATIVE -> IconResponse(IconType.NATIVE, value, color)
    NotionIconType.CUSTOM_EMOJI -> IconResponse(IconType.CUSTOM_EMOJI, value)
    null -> null
}
