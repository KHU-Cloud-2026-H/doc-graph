package com.docgraph.backend.graph.command.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

@Tag("unit")
class DependencyEdgeTest {

    @Test
    fun `create — 같은 프로젝트 내 방향성 의존 엣지를 생성`() {
        val now = OffsetDateTime.parse("2026-05-20T10:00:00+09:00")

        val edge = DependencyEdge(
            projectId = 1L,
            sourceDocumentId = 10L,
            targetDocumentId = 20L,
            ruleId = 30L,
            validationCriterion = "범위 일치 여부",
            source = DependencyEdgeSource.NOTION_REFERENCE,
            createdAt = now,
        )

        assertEquals(1L, edge.projectId)
        assertEquals(10L, edge.sourceDocumentId)
        assertEquals(20L, edge.targetDocumentId)
        assertEquals(30L, edge.ruleId)
        assertEquals("범위 일치 여부", edge.validationCriterion)
        assertEquals(DependencyEdgeSource.NOTION_REFERENCE, edge.source)
        assertEquals(EdgeConflictStatus.NONE, edge.conflictStatus)
        assertEquals(now, edge.createdAt)
        assertEquals(now, edge.updatedAt)
    }

    @Test
    fun `create — source와 target 문서는 달라야 함`() {
        assertThrows(IllegalArgumentException::class.java) {
            DependencyEdge(
                projectId = 1L,
                sourceDocumentId = 10L,
                targetDocumentId = 10L,
                validationCriterion = "범위 일치 여부",
                source = DependencyEdgeSource.CUSTOM,
            )
        }
    }

    @Test
    fun `markConflict and clearConflict — validation 결과에 맞춰 엣지 표시 상태를 변경`() {
        val createdAt = OffsetDateTime.parse("2026-05-20T10:00:00+09:00")
        val conflictedAt = OffsetDateTime.parse("2026-05-20T11:00:00+09:00")
        val resolvedAt = OffsetDateTime.parse("2026-05-20T12:00:00+09:00")
        val edge = DependencyEdge(
            projectId = 1L,
            sourceDocumentId = 10L,
            targetDocumentId = 20L,
            validationCriterion = "범위 일치 여부",
            source = DependencyEdgeSource.NOTION_REFERENCE,
            createdAt = createdAt,
        )

        edge.markConflict(conflictedAt)

        assertEquals(EdgeConflictStatus.CONFLICT, edge.conflictStatus)
        assertEquals(conflictedAt, edge.updatedAt)

        edge.clearConflict(resolvedAt)

        assertEquals(EdgeConflictStatus.NONE, edge.conflictStatus)
        assertEquals(resolvedAt, edge.updatedAt)
    }
}
