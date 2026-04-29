package com.docgraph.backend.validation.command.interfaces.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/conflicts")
@Tag(name = "Validation")
class ValidationCommandController {

    @PostMapping("/{id}/ignore")
    @Operation(
        summary = "충돌 수동 무시",
        description = "충돌을 무시 상태로 마킹한다. 무시된 충돌은 해당 문서 쌍 중 하나가 외부에서 변경되어 재검증이 실행되면 자동으로 해제된다.",
    )
    fun ignore(
        @PathVariable id: Long,
        @RequestBody request: IgnoreConflictRequest,
    ): ResponseEntity<Unit> {
        TODO()
    }

    @DeleteMapping("/{id}/ignore")
    @Operation(summary = "충돌 무시 해제")
    fun unignore(@PathVariable id: Long): ResponseEntity<Unit> {
        TODO()
    }
}

data class IgnoreConflictRequest(
    @Schema(description = "무시 사유 (선택)", example = "의도된 차이로 확인됨")
    val reason: String?,
)
