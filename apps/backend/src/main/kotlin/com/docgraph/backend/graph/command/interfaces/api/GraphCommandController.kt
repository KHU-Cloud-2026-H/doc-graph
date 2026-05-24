package com.docgraph.backend.graph.command.interfaces.api

import com.docgraph.backend.document.query.application.DocumentType
import com.docgraph.backend.graph.command.domain.DefaultGraphRuleCannotBeRemovedException
import com.docgraph.backend.graph.command.domain.DependencyEdgeNotFoundException
import com.docgraph.backend.graph.command.domain.EdgeProposalNotFoundException
import com.docgraph.backend.graph.command.domain.GraphRuleNotFoundException
import com.docgraph.backend.web.IdResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@Tag(name = "Graph")
class GraphCommandController {

    @PostMapping("/projects/{id}/edges")
    @Operation(summary = "커스텀 엣지 추가")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "엣지 생성 완료 — dependency_edge row id 반환"),
        ApiResponse(responseCode = "400", description = "source/target 문서 동일 등 요청 유효성 위반", content = [Content(schema = Schema(implementation = Void::class))]),
        ApiResponse(responseCode = "404", description = "프로젝트 또는 문서 없음", content = [Content(schema = Schema(implementation = Void::class))]),
        ApiResponse(responseCode = "409", description = "동일 방향 엣지가 이미 존재", content = [Content(schema = Schema(implementation = Void::class))]),
    ])
    fun createEdge(
        @PathVariable id: Long,
        @RequestBody request: CreateEdgeRequest,
    ): ResponseEntity<IdResponse> {
        TODO()
    }

    @DeleteMapping("/projects/{id}/edges/{edgeId}")
    @Operation(summary = "엣지 삭제")
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "삭제 완료"),
        ApiResponse(responseCode = "404", description = "엣지 없음 또는 다른 프로젝트 소속"),
    ])
    fun deleteEdge(
        @PathVariable id: Long,
        @PathVariable edgeId: Long,
    ): ResponseEntity<Unit> {
        TODO()
    }

    @PostMapping("/projects/{id}/proposals/{proposalId}/accept")
    @Operation(
        summary = "연결 제안 수락",
        description = "EdgeProposal을 DependencyEdge로 전환하고 정합성 검증을 즉시 트리거한다. 수락 후 해당 proposal은 삭제된다.",
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "수락 완료 — 생성된 dependency_edge row id 반환"),
        ApiResponse(responseCode = "404", description = "제안 없음 또는 다른 프로젝트 소속", content = [Content(schema = Schema(implementation = Void::class))]),
    ])
    fun acceptProposal(
        @PathVariable id: Long,
        @PathVariable proposalId: Long,
    ): ResponseEntity<IdResponse> {
        TODO()
    }

    @DeleteMapping("/projects/{id}/proposals/{proposalId}")
    @Operation(summary = "연결 제안 거절")
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "거절 완료"),
        ApiResponse(responseCode = "404", description = "제안 없음 또는 다른 프로젝트 소속"),
    ])
    fun rejectProposal(
        @PathVariable id: Long,
        @PathVariable proposalId: Long,
    ): ResponseEntity<Unit> {
        TODO()
    }

    @PostMapping("/projects/{id}/rules")
    @Operation(summary = "커스텀 룰 추가")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "룰 생성 완료 — graph_rule row id 반환"),
        ApiResponse(responseCode = "400", description = "source/target 타입 동일 등 요청 유효성 위반", content = [Content(schema = Schema(implementation = Void::class))]),
        ApiResponse(responseCode = "404", description = "프로젝트 없음", content = [Content(schema = Schema(implementation = Void::class))]),
        ApiResponse(responseCode = "409", description = "동일 (source_type, target_type) 룰이 프로젝트에 이미 존재", content = [Content(schema = Schema(implementation = Void::class))]),
    ])
    fun createRule(
        @PathVariable id: Long,
        @RequestBody request: CreateRuleRequest,
    ): ResponseEntity<IdResponse> {
        TODO()
    }

    @DeleteMapping("/projects/{id}/rules/{ruleId}")
    @Operation(summary = "룰 삭제")
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "삭제 완료"),
        ApiResponse(responseCode = "400", description = "기본 제공 룰 삭제 시도"),
        ApiResponse(responseCode = "404", description = "룰 없음 또는 다른 프로젝트 소속"),
    ])
    fun deleteRule(
        @PathVariable id: Long,
        @PathVariable ruleId: Long,
    ): ResponseEntity<Unit> {
        TODO()
    }

    @ExceptionHandler(
        DependencyEdgeNotFoundException::class,
        EdgeProposalNotFoundException::class,
        GraphRuleNotFoundException::class,
    )
    fun handleNotFound(): ResponseEntity<Unit> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).build()

    @ExceptionHandler(DefaultGraphRuleCannotBeRemovedException::class)
    fun handleBadRequest(): ResponseEntity<Unit> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
}

data class CreateEdgeRequest(
    @Schema(description = "출발 문서 ID (source 내용이 target에 반영되어야 함)", example = "1")
    val sourceDocumentId: Long,
    @Schema(description = "도착 문서 ID", example = "2")
    val targetDocumentId: Long,
    @Schema(description = "검증 기준", example = "범위 일치 여부")
    val validationCriterion: String,
)

data class CreateRuleRequest(
    @Schema(description = "출발 문서 타입")
    val sourceType: DocumentType,
    @Schema(description = "도착 문서 타입")
    val targetType: DocumentType,
    @Schema(description = "검증 기준", example = "범위 일치 여부")
    val validationCriterion: String,
)
