package com.docgraph.backend.validation.query.interfaces.api

import com.docgraph.backend.web.PageResponse
import com.docgraph.backend.validation.query.application.ConflictResponse
import com.docgraph.backend.validation.query.application.SearchConflictsByProjectQuery
import com.docgraph.backend.validation.query.application.SearchValidationTasksByProjectQuery
import com.docgraph.backend.validation.query.application.ValidationTaskResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@Tag(name = "Validation")
class ValidationQueryController(
    private val searchConflictsByProject: SearchConflictsByProjectQuery,
    private val searchValidationTasksByProject: SearchValidationTasksByProjectQuery,
) {

    @GetMapping("/projects/{id}/conflicts")
    @Operation(summary = "프로젝트 충돌 목록")
    fun listByProject(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<PageResponse<ConflictResponse>> {
        val result = searchConflictsByProject.search(id, PageRequest.of(page, size))
        return ResponseEntity.ok(result)
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

    @GetMapping("/projects/{id}/validation-tasks")
    @Operation(summary = "검증 작업 이력")
    fun listValidationTasks(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<PageResponse<ValidationTaskResponse>> {
        val result = searchValidationTasksByProject.search(id, PageRequest.of(page, size))
        return ResponseEntity.ok(result)
    }
}
