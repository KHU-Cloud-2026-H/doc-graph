package com.docgraph.backend.graph.command.interfaces.api

import com.docgraph.backend.common.DocumentType
import com.docgraph.backend.common.IdResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@Tag(name = "Graph")
class GraphCommandController {

    @PostMapping("/projects/{id}/edges")
    @Operation(summary = "커스텀 엣지 추가")
    fun createEdge(
        @PathVariable id: Long,
        @RequestBody request: CreateEdgeRequest,
    ): ResponseEntity<IdResponse> {
        TODO()
    }

    @DeleteMapping("/projects/{id}/edges/{edgeId}")
    @Operation(summary = "엣지 삭제")
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
    fun acceptProposal(
        @PathVariable id: Long,
        @PathVariable proposalId: Long,
    ): ResponseEntity<IdResponse> {
        TODO()
    }

    @DeleteMapping("/projects/{id}/proposals/{proposalId}")
    @Operation(summary = "연결 제안 거절")
    fun rejectProposal(
        @PathVariable id: Long,
        @PathVariable proposalId: Long,
    ): ResponseEntity<Unit> {
        TODO()
    }

    @PostMapping("/projects/{id}/rules")
    @Operation(summary = "커스텀 룰 추가")
    fun createRule(
        @PathVariable id: Long,
        @RequestBody request: CreateRuleRequest,
    ): ResponseEntity<IdResponse> {
        TODO()
    }

    @DeleteMapping("/projects/{id}/rules/{ruleId}")
    @Operation(summary = "룰 삭제")
    fun deleteRule(
        @PathVariable id: Long,
        @PathVariable ruleId: Long,
    ): ResponseEntity<Unit> {
        TODO()
    }
}

data class CreateEdgeRequest(
    @Schema(description = "출발 문서 ID (source 내용이 target에 반영되어야 함)", example = "1")
    val sourceDocumentId: Long,
    @Schema(description = "도착 문서 ID", example = "2")
    val targetDocumentId: Long,
    @Schema(description = "체크 항목", example = "범위 일치 여부")
    val checkItem: String,
)

data class CreateRuleRequest(
    @Schema(description = "출발 문서 타입")
    val sourceType: DocumentType,
    @Schema(description = "도착 문서 타입")
    val targetType: DocumentType,
    @Schema(description = "체크 항목", example = "범위 일치 여부")
    val checkItem: String,
)
