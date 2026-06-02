package com.docgraph.backend.validation.query.interfaces.api

import com.docgraph.backend.web.PageResponse
import com.docgraph.backend.validation.query.application.AiUsageRecordResponse
import com.docgraph.backend.validation.query.application.AiUsageSummaryResponse
import com.docgraph.backend.validation.query.application.ConflictResponse
import com.docgraph.backend.validation.query.application.GetAiUsageSummaryByProjectQuery
import com.docgraph.backend.validation.query.application.MyConflictRow
import com.docgraph.backend.validation.query.application.MyConflictStatusFilter
import com.docgraph.backend.validation.query.application.SearchAiUsageRecordsByProjectQuery
import com.docgraph.backend.validation.query.application.SearchConflictsByProjectQuery
import com.docgraph.backend.validation.query.application.SearchMyConflictsQuery
import com.docgraph.backend.validation.query.application.SearchValidationTasksByProjectQuery
import com.docgraph.backend.validation.query.application.ValidationTaskResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@Tag(name = "Validation")
class ValidationQueryController(
    private val searchConflictsByProject: SearchConflictsByProjectQuery,
    private val searchValidationTasksByProject: SearchValidationTasksByProjectQuery,
    private val searchMyConflicts: SearchMyConflictsQuery,
    private val getAiUsageSummaryByProject: GetAiUsageSummaryByProjectQuery,
    private val searchAiUsageRecordsByProject: SearchAiUsageRecordsByProjectQuery,
) {

    @GetMapping("/projects/{id}/conflicts")
    @Operation(summary = "프로젝트 충돌 목록")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "충돌 목록 (없으면 빈 page)"),
    ])
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
        description = "내가 담당자인 문서가 target인 충돌 목록을 배정된 모든 프로젝트에 걸쳐 반환한다. " +
            "라우팅: (1) 직접 담당 (Document.assigneeMemberId), (2) type 매핑 (TypeAssigneeDefault), " +
            "(3) Admin 프로젝트의 미할당 문서. status=ACTIVE(default)|IGNORED|ALL.",
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "인박스 목록 (없으면 빈 page)"),
    ])
    fun myInbox(
        @RequestParam(defaultValue = "ACTIVE") status: MyConflictStatusFilter,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<PageResponse<MyConflictRow>> {
        val result = searchMyConflicts.search(status, PageRequest.of(page, size))
        return ResponseEntity.ok(result)
    }

    @GetMapping("/projects/{id}/validation-tasks")
    @Operation(summary = "검증 작업 이력")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "검증 작업 이력 page"),
    ])
    fun listValidationTasks(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<PageResponse<ValidationTaskResponse>> {
        val result = searchValidationTasksByProject.search(id, PageRequest.of(page, size))
        return ResponseEntity.ok(result)
    }

    @GetMapping("/projects/{id}/ai-usage")
    @Operation(summary = "AI 사용량 요약", description = "프로젝트의 총 호출·토큰 합계와 모델별 분해.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "사용량 요약 (없으면 0·빈 분해)"),
    ])
    fun getAiUsageSummary(@PathVariable id: Long): ResponseEntity<AiUsageSummaryResponse> {
        return ResponseEntity.ok(getAiUsageSummaryByProject.get(id))
    }

    @GetMapping("/projects/{id}/ai-usage/records")
    @Operation(summary = "AI 사용량 호출 이력", description = "검증 작업별 토큰·모델 호출 이력.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "호출 이력 page (없으면 빈 page)"),
    ])
    fun listAiUsageRecords(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<PageResponse<AiUsageRecordResponse>> {
        val result = searchAiUsageRecordsByProject.search(id, PageRequest.of(page, size))
        return ResponseEntity.ok(result)
    }
}
