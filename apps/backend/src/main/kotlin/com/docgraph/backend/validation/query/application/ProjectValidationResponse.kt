package com.docgraph.backend.validation.query.application

import io.swagger.v3.oas.annotations.media.Schema

data class ProjectValidationResponse(
    @Schema(description = "정합성 검증 활성 여부 (설정 부재 시 기본 true)", example = "true")
    val enabled: Boolean,
)
