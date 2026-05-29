package com.docgraph.backend.validation.command.application

import com.docgraph.backend.validation.command.domain.Conflict
import com.docgraph.backend.validation.command.domain.ConflictDetectedEvent
import com.docgraph.backend.validation.command.domain.ConflictFinding
import com.docgraph.backend.validation.command.domain.ConflictFindingRepository
import com.docgraph.backend.validation.command.domain.ConflictReconciler
import com.docgraph.backend.validation.command.domain.ConflictRepository
import com.docgraph.backend.validation.command.domain.ConflictResolvedEvent
import com.docgraph.backend.validation.command.domain.DetectedConflict
import com.docgraph.backend.validation.command.domain.ReconcileOutcome
import com.docgraph.backend.validation.command.domain.ValidationTaskRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class CompleteValidationTaskCommandHandler(
    private val taskRepository: ValidationTaskRepository,
    private val conflictRepository: ConflictRepository,
    private val findingRepository: ConflictFindingRepository,
    private val reconciler: ConflictReconciler,
    private val publisher: ApplicationEventPublisher,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(command: CompleteValidationTaskCommand) {
        val task = taskRepository.findById(command.validationTaskId)
            ?: error("validation task not found: ${command.validationTaskId}")
        val now = OffsetDateTime.now()

        val existing = conflictRepository.findFirstByEdgeIdAndResolvedAtIsNull(task.edgeId)
        val outcome = reconciler.reconcile(existing, command.findings, task.edgeId, now)

        applyOutcome(outcome, taskId = task.id, edgeId = task.edgeId, now = now)
        task.markSuccess()
    }

    private fun applyOutcome(outcome: ReconcileOutcome, taskId: Long, edgeId: Long, now: OffsetDateTime) {
        when (outcome) {
            ReconcileOutcome.NoOp -> Unit
            is ReconcileOutcome.Detected -> {
                val saved = conflictRepository.save(outcome.newConflict)
                persistFindings(outcome.findings, conflictId = saved.id, taskId = taskId, now = now)
                publisher.publishEvent(ConflictDetectedEvent(saved.id, edgeId, now))
            }
            is ReconcileOutcome.Updated -> {
                persistFindings(outcome.findings, conflictId = outcome.conflict.id, taskId = taskId, now = now)
            }
            is ReconcileOutcome.Resolved -> {
                publisher.publishEvent(ConflictResolvedEvent(outcome.conflict.id, edgeId, now))
            }
        }
    }

    private fun persistFindings(
        findings: List<DetectedConflict>,
        conflictId: Long,
        taskId: Long,
        now: OffsetDateTime,
    ) {
        findings.forEach { f ->
            findingRepository.save(
                ConflictFinding(
                    conflictId = conflictId,
                    validationTaskId = taskId,
                    sourceBlockIds = f.sourceBlockIds,
                    targetBlockId = f.targetBlockId,
                    rationale = f.rationale,
                    newText = f.newText,
                    title = f.title,
                    detectedAt = now,
                ),
            )
        }
    }
}
