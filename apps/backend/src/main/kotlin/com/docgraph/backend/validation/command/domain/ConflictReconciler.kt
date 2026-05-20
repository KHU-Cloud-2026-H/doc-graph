package com.docgraph.backend.validation.command.domain

import org.springframework.stereotype.Component
import java.time.OffsetDateTime

@Component
class ConflictReconciler {
    fun reconcile(
        existing: Conflict?,
        findings: List<DetectedConflict>,
        edgeId: Long,
        now: OffsetDateTime,
    ): ReconcileOutcome {
        if (findings.isEmpty()) {
            return if (existing == null) {
                ReconcileOutcome.NoOp
            } else {
                existing.markResolved(now)
                ReconcileOutcome.Resolved(existing)
            }
        }
        return if (existing == null) {
            ReconcileOutcome.Detected(
                newConflict = Conflict(edgeId = edgeId, firstDetectedAt = now, lastDetectedAt = now),
                findings = findings,
            )
        } else {
            existing.markDetected(now)
            ReconcileOutcome.Updated(conflict = existing, findings = findings)
        }
    }
}

sealed class ReconcileOutcome {
    data object NoOp : ReconcileOutcome()
    data class Detected(val newConflict: Conflict, val findings: List<DetectedConflict>) : ReconcileOutcome()
    data class Updated(val conflict: Conflict, val findings: List<DetectedConflict>) : ReconcileOutcome()
    data class Resolved(val conflict: Conflict) : ReconcileOutcome()
}
