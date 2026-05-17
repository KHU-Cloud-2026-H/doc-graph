package com.docgraph.backend.validation.command.domain

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Tag("unit")
class ConflictFindingTest {

    @Test
    fun `초기 — isApproved false, approvedAt·approvedBy null`() {
        val finding = newFinding()

        assertFalse(finding.isApproved)
        assertNull(finding.approvedAt)
        assertNull(finding.approvedBy)
    }

    @Test
    fun `approve — approvedAt·approvedBy 세팅 + isApproved true`() {
        val finding = newFinding()
        val at = OffsetDateTime.parse("2026-05-17T10:00:00Z")

        finding.approve(by = 42L, at = at)

        assertTrue(finding.isApproved)
        assertEquals(at, finding.approvedAt)
        assertEquals(42L, finding.approvedBy)
    }

    @Test
    fun `이미 승인된 finding에 approve 재호출 — 예외`() {
        val finding = newFinding().apply {
            approve(by = 1L, at = OffsetDateTime.now())
        }

        assertThrows<IllegalConflictFindingStateException> {
            finding.approve(by = 2L, at = OffsetDateTime.now())
        }
    }

    private fun newFinding(): ConflictFinding = ConflictFinding(
        conflictId = 1L,
        validationTaskId = 1L,
        sourceBlockIds = listOf("s"),
        targetBlockIds = listOf("t"),
        rationale = "r",
        suggestion = "sug",
        detectedAt = OffsetDateTime.now(),
    )
}
