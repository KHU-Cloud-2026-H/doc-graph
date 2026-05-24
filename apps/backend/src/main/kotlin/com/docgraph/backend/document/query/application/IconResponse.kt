package com.docgraph.backend.document.query.application

import io.swagger.v3.oas.annotations.media.Schema

enum class IconType {
    EMOJI,
    EXTERNAL,
    FILE,
}

data class IconResponse(
    @Schema(description = "아이콘 타입 — EMOJI는 이모지 문자, EXTERNAL은 외부 URL, FILE은 Notion signed URL(만료 가능)")
    val type: IconType,
    @Schema(description = "타입에 따른 값 — emoji면 이모지 문자, external/file이면 URL", example = "📄")
    val value: String,
)