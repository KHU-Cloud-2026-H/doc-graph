package com.docgraph.backend.document.query.application

import io.swagger.v3.oas.annotations.media.Schema

enum class IconType {
    EMOJI,
    EXTERNAL,
    FILE,
    NATIVE,
    CUSTOM_EMOJI,
}

data class IconResponse(
    @Schema(
        description = "아이콘 타입 — EMOJI는 이모지 문자, EXTERNAL은 외부 URL, FILE은 Notion signed URL(만료 가능), " +
            "NATIVE는 Notion 빌트인 아이콘 이름, CUSTOM_EMOJI는 커스텀 이모지 이미지 URL",
    )
    val type: IconType,
    @Schema(
        description = "타입에 따른 값 — EMOJI면 이모지 문자, EXTERNAL/FILE/CUSTOM_EMOJI면 URL, NATIVE면 아이콘 이름",
        example = "📄",
    )
    val value: String,
    @Schema(description = "NATIVE 아이콘 색상 (gray/blue 등). 그 외 타입은 null", example = "gray")
    val color: String? = null,
)