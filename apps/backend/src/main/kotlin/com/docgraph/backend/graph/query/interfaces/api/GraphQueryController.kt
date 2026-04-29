package com.docgraph.backend.graph.query.interfaces.api

import com.docgraph.backend.graph.query.application.EdgeProposalResponse
import com.docgraph.backend.graph.query.application.EdgeResponse
import com.docgraph.backend.graph.query.application.ProjectGraphResponse
import com.docgraph.backend.graph.query.application.RuleResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@Tag(name = "Graph")
class GraphQueryController {

    @GetMapping("/projects/{id}/graph")
    @Operation(
        summary = "그래프 전체 데이터 (캔버스 초기 렌더링용)",
        description = "캔버스 초기 렌더링에 필요한 nodes·edges·proposals를 한 번에 반환한다. edges는 실선 의존 관계, proposals는 점선 연결 후보다. 패널·관리 목록이 필요할 때는 개별 list API를 사용한다.",
    )
    fun getGraph(@PathVariable id: Long): ResponseEntity<ProjectGraphResponse> {
        TODO()
    }

    @GetMapping("/projects/{id}/edges")
    @Operation(summary = "엣지 목록 (관리 패널용)")
    fun listEdges(@PathVariable id: Long): ResponseEntity<List<EdgeResponse>> {
        TODO()
    }

    @GetMapping("/projects/{id}/proposals")
    @Operation(summary = "연결 제안 목록 (관리 패널용)")
    fun listProposals(@PathVariable id: Long): ResponseEntity<List<EdgeProposalResponse>> {
        TODO()
    }

    @GetMapping("/projects/{id}/rules")
    @Operation(summary = "룰 목록")
    fun listRules(@PathVariable id: Long): ResponseEntity<List<RuleResponse>> {
        TODO()
    }
}
