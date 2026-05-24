package com.docgraph.backend.acceptance

import com.docgraph.backend.graph.command.domain.DependencyEdge
import com.docgraph.backend.graph.command.domain.DependencyEdgeSource
import com.docgraph.backend.graph.command.domain.EdgeProposal
import com.docgraph.backend.web.IdResponse
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * acceptance profile 한정 — UC4·UC5·UC6·UC8 spec이 graph 상태를 직접 seed하는 우회 진입점.
 *
 * 운영 흐름은 document 이벤트 수신 + 룰 기반 자동 엣지/제안 생성. fixture는 EntityManager
 * 직접 persist — graph 도메인 JPA Repository 구현체 미존재라 Repository interface DI 불가.
 *
 * 시드 요청은 entity invariants를 그대로 반영 — 필수 필드는 require, 선택 필드만 nullable.
 */
@RestController
@Profile("acceptance")
class GraphFixtureController(
    @PersistenceContext private val em: EntityManager,
) {

    @PostMapping("/test/edges")
    @Transactional
    fun seedEdge(@RequestBody request: SeedEdgeRequest): ResponseEntity<IdResponse> {
        val edge = DependencyEdge(
            projectId = request.projectId,
            sourceDocumentId = request.sourceDocumentId,
            targetDocumentId = request.targetDocumentId,
            ruleId = request.ruleId,
            validationCriterion = request.validationCriterion,
            source = request.source,
        )
        em.persist(edge)
        return ResponseEntity.ok(IdResponse(edge.id))
    }

    @PostMapping("/test/proposals")
    @Transactional
    fun seedProposal(@RequestBody request: SeedProposalRequest): ResponseEntity<IdResponse> {
        val proposal = EdgeProposal(
            projectId = request.projectId,
            sourceDocumentId = request.sourceDocumentId,
            targetDocumentId = request.targetDocumentId,
            ruleId = request.ruleId,
            validationCriterion = request.validationCriterion,
            similarityScore = request.similarityScore,
        )
        em.persist(proposal)
        return ResponseEntity.ok(IdResponse(proposal.id))
    }
}

data class SeedEdgeRequest(
    @Schema(example = "1")
    val projectId: Long,
    @Schema(example = "10")
    val sourceDocumentId: Long,
    @Schema(example = "20")
    val targetDocumentId: Long,
    @Schema(example = "범위 일치 여부")
    val validationCriterion: String,
    @Schema(description = "엣지 생성 출처")
    val source: DependencyEdgeSource,
    @Schema(description = "연관 룰 ID (수동 추가 엣지는 null)", example = "1")
    val ruleId: Long? = null,
)

data class SeedProposalRequest(
    @Schema(example = "1")
    val projectId: Long,
    @Schema(example = "10")
    val sourceDocumentId: Long,
    @Schema(example = "20")
    val targetDocumentId: Long,
    @Schema(example = "결정사항 반영 여부")
    val validationCriterion: String,
    @Schema(example = "0.85")
    val similarityScore: Double,
    @Schema(description = "연관 룰 ID", example = "1")
    val ruleId: Long? = null,
)
