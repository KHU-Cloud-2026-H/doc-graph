package com.docgraph.backend.graph.command.domain

import com.docgraph.backend.document.query.application.DocumentType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

@Tag("unit")
class GraphRuleTest {

    @Test
    fun `create — 기본 룰은 프로젝트 없이 전역 룰로 생성`() {
        val now = OffsetDateTime.parse("2026-05-20T10:00:00+09:00")

        val rule = GraphRule(
            sourceType = DocumentType.PLANNING,
            targetType = DocumentType.REQUIREMENTS,
            validationCriterion = "범위 일치 여부",
            isDefault = true,
            createdAt = now,
        )

        assertNull(rule.projectId)
        assertEquals(DocumentType.PLANNING, rule.sourceType)
        assertEquals(DocumentType.REQUIREMENTS, rule.targetType)
        assertEquals("범위 일치 여부", rule.validationCriterion)
        assertTrue(rule.isDefault)
        assertEquals(now, rule.createdAt)
    }

    @Test
    fun `create — 커스텀 룰은 프로젝트에 속해야 함`() {
        assertThrows(IllegalArgumentException::class.java) {
            GraphRule(
                sourceType = DocumentType.PLANNING,
                targetType = DocumentType.REQUIREMENTS,
                validationCriterion = "범위 일치 여부",
                isDefault = false,
            )
        }
    }

    @Test
    fun `create — 기본 룰은 프로젝트에 속할 수 없음`() {
        assertThrows(IllegalArgumentException::class.java) {
            GraphRule(
                projectId = 1L,
                sourceType = DocumentType.PLANNING,
                targetType = DocumentType.REQUIREMENTS,
                validationCriterion = "범위 일치 여부",
                isDefault = true,
            )
        }
    }

    @Test
    fun `appliesTo — source target 타입 조합이 일치할 때만 룰 적용`() {
        val rule = GraphRule(
            projectId = 1L,
            sourceType = DocumentType.REQUIREMENTS,
            targetType = DocumentType.DESIGN,
            validationCriterion = "스펙 일치 여부",
        )

        assertTrue(rule.appliesTo(DocumentType.REQUIREMENTS, DocumentType.DESIGN))
        assertFalse(rule.appliesTo(DocumentType.DESIGN, DocumentType.REQUIREMENTS))
        assertFalse(rule.appliesTo(DocumentType.REQUIREMENTS, null))
    }

    @Test
    fun `toEdge — 룰 기준으로 검증 가능한 의존 엣지 생성`() {
        val now = OffsetDateTime.parse("2026-05-20T10:00:00+09:00")
        val rule = GraphRule(
            id = 30L,
            projectId = 1L,
            sourceType = DocumentType.REQUIREMENTS,
            targetType = DocumentType.DESIGN,
            validationCriterion = "스펙 일치 여부",
        )

        val edge = rule.toEdge(
            projectId = 1L,
            sourceDocumentId = 10L,
            targetDocumentId = 20L,
            source = DependencyEdgeSource.CUSTOM,
            at = now,
        )

        assertEquals(1L, edge.projectId)
        assertEquals(10L, edge.sourceDocumentId)
        assertEquals(20L, edge.targetDocumentId)
        assertEquals(30L, edge.ruleId)
        assertEquals("스펙 일치 여부", edge.validationCriterion)
        assertEquals(DependencyEdgeSource.CUSTOM, edge.source)
        assertEquals(now, edge.createdAt)
        assertEquals(now, edge.updatedAt)
    }
}
