package com.docgraph.backend.graph.command.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

@Tag("unit")
class EdgeProposalTest {

    @Test
    fun `create — 키워드 매칭 기반 연결 제안을 생성`() {
        val now = OffsetDateTime.parse("2026-05-20T10:00:00+09:00")

        val proposal = EdgeProposal(
            projectId = 1L,
            sourceDocumentId = 10L,
            targetDocumentId = 20L,
            ruleId = 30L,
            validationCriterion = "결정사항 반영 여부",
            similarityScore = 0.82,
            createdAt = now,
        )

        assertEquals(1L, proposal.projectId)
        assertEquals(10L, proposal.sourceDocumentId)
        assertEquals(20L, proposal.targetDocumentId)
        assertEquals(30L, proposal.ruleId)
        assertEquals("결정사항 반영 여부", proposal.validationCriterion)
        assertEquals(0.82, proposal.similarityScore)
        assertEquals(now, proposal.createdAt)
    }

    @Test
    fun `create — similarityScore는 0 이상 1 이하`() {
        assertThrows(IllegalArgumentException::class.java) {
            EdgeProposal(
                projectId = 1L,
                sourceDocumentId = 10L,
                targetDocumentId = 20L,
                validationCriterion = "결정사항 반영 여부",
                similarityScore = 1.1,
            )
        }
    }

    @Test
    fun `accept — 제안 수락 시 검증 가능한 의존 엣지로 전환`() {
        val acceptedAt = OffsetDateTime.parse("2026-05-20T11:00:00+09:00")
        val proposal = EdgeProposal(
            projectId = 1L,
            sourceDocumentId = 10L,
            targetDocumentId = 20L,
            ruleId = 30L,
            validationCriterion = "결정사항 반영 여부",
            similarityScore = 0.82,
        )

        val edge = proposal.accept(acceptedAt)

        assertEquals(proposal.projectId, edge.projectId)
        assertEquals(proposal.sourceDocumentId, edge.sourceDocumentId)
        assertEquals(proposal.targetDocumentId, edge.targetDocumentId)
        assertEquals(proposal.ruleId, edge.ruleId)
        assertEquals(proposal.validationCriterion, edge.validationCriterion)
        assertEquals(DependencyEdgeSource.PROPOSAL_ACCEPTED, edge.source)
        assertEquals(EdgeConflictStatus.NONE, edge.conflictStatus)
        assertEquals(acceptedAt, edge.createdAt)
        assertEquals(acceptedAt, edge.updatedAt)
    }
}
