package com.docgraph.backend.validation.command.domain

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@Tag("unit")
class ConflictReconcilerTest {
    private val reconciler = ConflictReconciler()

    @Test
    fun `existing 없음 + findings 없음 — NoOp`() {
        val outcome = reconciler.reconcile(
            existing = null,
            findings = emptyList(),
            edgeId = 1L,
            now = OffsetDateTime.now(),
        )

        assertSame(ReconcileOutcome.NoOp, outcome)
    }

    @Test
    fun `existing 없음 + findings 있음 — Detected (새 Conflict 생성)`() {
        val now = OffsetDateTime.parse("2026-05-15T00:00:00Z")
        val findings = listOf(sampleFinding())

        val outcome = reconciler.reconcile(existing = null, findings = findings, edgeId = 7L, now = now)

        val detected = assertIs<ReconcileOutcome.Detected>(outcome)
        assertEquals(7L, detected.newConflict.edgeId)
        assertEquals(now, detected.newConflict.firstDetectedAt)
        assertEquals(now, detected.newConflict.lastDetectedAt)
        assertNull(detected.newConflict.resolvedAt)
        assertNull(detected.newConflict.ignoredAt)
        assertEquals(findings, detected.findings)
    }

    @Test
    fun `existing 있음 + findings 없음 — Resolved (markResolved 호출)`() {
        val older = OffsetDateTime.parse("2026-05-10T00:00:00Z")
        val now = OffsetDateTime.parse("2026-05-15T00:00:00Z")
        val existing = Conflict(edgeId = 9L, firstDetectedAt = older, lastDetectedAt = older)

        val outcome = reconciler.reconcile(existing = existing, findings = emptyList(), edgeId = 9L, now = now)

        val resolved = assertIs<ReconcileOutcome.Resolved>(outcome)
        assertSame(existing, resolved.conflict)
        assertEquals(now, resolved.conflict.resolvedAt)
        assertTrue(!resolved.conflict.isActive)
    }

    @Test
    fun `existing 있음(ignored) + findings 있음 — Updated (markDetected로 ignored 해제)`() {
        val older = OffsetDateTime.parse("2026-05-10T00:00:00Z")
        val now = OffsetDateTime.parse("2026-05-15T00:00:00Z")
        val existing = Conflict(edgeId = 11L, firstDetectedAt = older, lastDetectedAt = older).apply {
            ignore(by = 42L, reason = "의도된 차이", at = older)
        }
        val findings = listOf(sampleFinding())

        val outcome = reconciler.reconcile(existing = existing, findings = findings, edgeId = 11L, now = now)

        val updated = assertIs<ReconcileOutcome.Updated>(outcome)
        assertSame(existing, updated.conflict)
        assertEquals(now, updated.conflict.lastDetectedAt)
        assertEquals(older, updated.conflict.firstDetectedAt) // 첫 감지 시각은 보존
        assertNull(updated.conflict.ignoredAt)
        assertNull(updated.conflict.ignoredBy)
        assertNull(updated.conflict.ignoreReason)
        assertEquals(findings, updated.findings)
    }

    @Test
    fun `existing 있음(미ignored) + findings 있음 — Updated`() {
        val older = OffsetDateTime.parse("2026-05-10T00:00:00Z")
        val now = OffsetDateTime.parse("2026-05-15T00:00:00Z")
        val existing = Conflict(edgeId = 13L, firstDetectedAt = older, lastDetectedAt = older)

        val outcome = reconciler.reconcile(
            existing = existing,
            findings = listOf(sampleFinding()),
            edgeId = 13L,
            now = now,
        )

        val updated = assertIs<ReconcileOutcome.Updated>(outcome)
        assertEquals(now, updated.conflict.lastDetectedAt)
        assertNotNull(updated.conflict) // sanity
    }

    private fun sampleFinding() = DetectedConflict(
        sourceBlockIds = listOf("s1"),
        targetBlockId = "t1",
        rationale = "rationale",
        newText = "suggestion",
        title = "title",
    )
}
