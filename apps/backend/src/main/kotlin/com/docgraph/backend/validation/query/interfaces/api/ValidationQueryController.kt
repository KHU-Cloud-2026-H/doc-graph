package com.docgraph.backend.validation.query.interfaces.api

import com.docgraph.backend.common.PageResponse
import com.docgraph.backend.validation.query.application.ConflictResponse
import com.docgraph.backend.validation.query.application.ValidationRunResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@Tag(name = "Validation")
class ValidationQueryController {

    @GetMapping("/projects/{id}/conflicts")
    @Operation(summary = "프로젝트 충돌 목록")
    fun listByProject(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<PageResponse<ConflictResponse>> {
        TODO()
    }

    @GetMapping("/me/conflicts")
    @Operation(
        summary = "내 인박스",
        description = "내가 담당자인 문서가 target인 충돌 목록을 배정된 모든 프로젝트에 걸쳐 반환한다. 미해소(CONFLICT) 상태가 기본이며, 무시 포함 전체 조회는 status 파라미터로 확장 예정(Post-MVP).",
    )
    fun myInbox(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<PageResponse<ConflictResponse>> {
        TODO()
    }

    @GetMapping("/projects/{id}/validation-runs")
    @Operation(summary = "검증 실행 이력")
    fun listValidationRuns(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<PageResponse<ValidationRunResponse>> {
        TODO()
    }
}
