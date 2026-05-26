package com.docgraph.backend.validation.command.interfaces.event

import com.docgraph.backend.document.query.application.FindDocumentByIdQuery
import com.docgraph.backend.event.OutboxStatus
import com.docgraph.backend.graph.query.application.FindEdgeByIdQuery
import com.docgraph.backend.validation.command.application.CompleteValidationTaskCommand
import com.docgraph.backend.validation.command.application.CompleteValidationTaskCommandHandler
import com.docgraph.backend.validation.command.application.ValidationTaskTransitionService
import com.docgraph.backend.validation.command.domain.ConflictDetector
import com.docgraph.backend.validation.command.domain.FirstValidationInput
import com.docgraph.backend.validation.command.domain.ValidationTaskPreparedEvent
import com.docgraph.backend.validation.command.domain.ValidationTaskRepository
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Recover
import org.springframework.retry.annotation.Retryable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ValidationTaskPreparedEventListener(
    private val taskRepository: ValidationTaskRepository,
    private val findEdgeById: FindEdgeByIdQuery,
    private val findDocumentById: FindDocumentByIdQuery,
    private val detector: ConflictDetector,
    private val completeHandler: CompleteValidationTaskCommandHandler,
    private val transition: ValidationTaskTransitionService,
) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(
        maxAttemptsExpression = "\${validation.task.detect.max-attempts:5}",
        backoff = Backoff(
            delayExpression = "\${validation.task.detect.retry-delay-ms:1000}",
            multiplierExpression = "\${validation.task.detect.retry-multiplier:2.0}",
            maxDelayExpression = "\${validation.task.detect.retry-max-delay-ms:60000}",
            random = true,
        ),
    )
    fun on(event: ValidationTaskPreparedEvent) {
        val task = taskRepository.findById(event.validationTaskId)
            ?: error("validation task not found: ${event.validationTaskId}")
        if (task.status != OutboxStatus.PENDING) return

        val edge = findEdgeById.find(task.edgeId)
            ?: error("edge not found: ${task.edgeId}")
        val source = findDocumentById.find(edge.sourceDocumentId)
            ?: error("source document not found: ${edge.sourceDocumentId}")
        val target = findDocumentById.find(edge.targetDocumentId)
            ?: error("target document not found: ${edge.targetDocumentId}")

        val findings = detector.detect(
            FirstValidationInput(
                sourceBlocks = source.blocks,
                targetBlocks = target.blocks,
                criterion = edge.validationCriterion,
            ),
        )
        completeHandler.handle(CompleteValidationTaskCommand(task.id, findings))
    }

    @Recover
    fun recover(ex: Throwable, event: ValidationTaskPreparedEvent) {
        transition.markFailed(event.validationTaskId, ex.message)
    }
}
