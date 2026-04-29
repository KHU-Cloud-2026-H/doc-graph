package com.docgraph.backend.common

import io.swagger.v3.oas.annotations.media.Schema

data class IdResponse(
    @Schema(description = "생성된 리소스 ID", example = "1")
    val id: Long,
)
