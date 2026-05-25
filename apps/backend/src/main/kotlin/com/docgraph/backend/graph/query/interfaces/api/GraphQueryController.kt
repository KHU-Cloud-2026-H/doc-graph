package com.docgraph.backend.graph.query.interfaces.api

import com.docgraph.backend.auth.query.application.GetCurrentUserIdQuery
import com.docgraph.backend.graph.query.application.EdgeProposalResponse
import com.docgraph.backend.graph.query.application.EdgeResponse
import com.docgraph.backend.graph.query.application.FindProjectGraphQuery
import com.docgraph.backend.graph.query.application.ProjectGraphResponse
import com.docgraph.backend.graph.query.application.RuleResponse
import com.docgraph.backend.graph.query.application.SearchEdgeProposalsByProjectQuery
import com.docgraph.backend.graph.query.application.SearchEdgesByProjectQuery
import com.docgraph.backend.graph.query.application.SearchGraphRulesByProjectQuery
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@Tag(name = "Graph")
class GraphQueryController(
    private val findProjectGraph: FindProjectGraphQuery,
    private val searchEdgesByProject: SearchEdgesByProjectQuery,
    private val searchEdgeProposalsByProject: SearchEdgeProposalsByProjectQuery,
    private val searchGraphRulesByProject: SearchGraphRulesByProjectQuery,
    private val getCurrentUserId: GetCurrentUserIdQuery,
) {

    @GetMapping("/projects/{id}/graph")
    @Operation(
        summary = "그래프 전체 데이터 (캔버스 초기 렌더링용)",
        description = "캔버스 초기 렌더링에 필요한 nodes·edges·proposals를 한 번에 반환한다. edges는 실선 의존 관계, proposals는 점선 연결 후보다. 패널·관리 목록이 필요할 때는 개별 list API를 사용한다.",
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "그래프 데이터"),
        ApiResponse(responseCode = "404", description = "프로젝트 없음"),
    ])
    fun getGraph(@PathVariable id: Long): ResponseEntity<ProjectGraphResponse> {
        getCurrentUserId.get()
        return ResponseEntity.ok(findProjectGraph.find(id))
    }

    @GetMapping("/projects/{id}/edges")
    @Operation(summary = "엣지 목록 (관리 패널용)")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "엣지 목록"),
        ApiResponse(responseCode = "404", description = "프로젝트 없음"),
    ])
    fun listEdges(@PathVariable id: Long): ResponseEntity<List<EdgeResponse>> {
        getCurrentUserId.get()
        return ResponseEntity.ok(searchEdgesByProject.search(id))
    }

    @GetMapping("/projects/{id}/proposals")
    @Operation(summary = "연결 제안 목록 (관리 패널용)")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "연결 제안 목록"),
        ApiResponse(responseCode = "404", description = "프로젝트 없음"),
    ])
    fun listProposals(@PathVariable id: Long): ResponseEntity<List<EdgeProposalResponse>> {
        getCurrentUserId.get()
        return ResponseEntity.ok(searchEdgeProposalsByProject.search(id))
    }

    @GetMapping("/projects/{id}/rules")
    @Operation(
        summary = "룰 목록",
        description = "프로젝트에 적용 가능한 룰(기본 제공 + 커스텀)을 반환한다.",
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "룰 목록 (기본 + 커스텀)"),
        ApiResponse(responseCode = "404", description = "프로젝트 없음"),
    ])
    fun listRules(@PathVariable id: Long): ResponseEntity<List<RuleResponse>> {
        getCurrentUserId.get()
        return ResponseEntity.ok(searchGraphRulesByProject.search(id))
    }
}
