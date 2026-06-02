package com.docgraph.backend.validation.command.application

import com.docgraph.backend.document.query.application.FindDocumentByIdQuery
import com.docgraph.backend.event.OutboxStatus
import com.docgraph.backend.graph.query.application.FindEdgeByIdQuery
import com.docgraph.backend.validation.command.domain.FailureCategory
import com.docgraph.backend.validation.command.domain.ValidationTaskPreparedEvent
import com.docgraph.backend.validation.command.domain.ValidationTaskRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Recover
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class ProcessValidationTaskCommandHandler(
    private val repository: ValidationTaskRepository,
    private val transition: ValidationTaskTransitionService,
    private val findEdgeById: FindEdgeByIdQuery,
    private val findDocumentById: FindDocumentByIdQuery,
    private val publisher: ApplicationEventPublisher,
) {
    @Retryable(
        maxAttemptsExpression = "\${validation.task.process.max-attempts:5}",
        backoff = Backoff(
            delayExpression = "\${validation.task.process.retry-delay-ms:1000}",
            multiplierExpression = "\${validation.task.process.retry-multiplier:2.0}",
            maxDelayExpression = "\${validation.task.process.retry-max-delay-ms:60000}",
            random = true,
        ),
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(command: ProcessValidationTaskCommand) {
        transition.recordAttempt(command.taskId)

        val task = repository.findById(command.taskId)
            ?: error("validation task not found: ${command.taskId}")
        if (task.status != OutboxStatus.PENDING) return

        val edge = findEdgeById.find(task.edgeId) ?: run {
            transition.markFailed(command.taskId, FailureCategory.MISSING_REFERENCE, "edge not found: ${task.edgeId}")
            return
        }
        findDocumentById.find(edge.sourceDocumentId) ?: run {
            transition.markFailed(
                command.taskId, FailureCategory.MISSING_REFERENCE, "source document not found: ${edge.sourceDocumentId}",
            )
            return
        }
        findDocumentById.find(edge.targetDocumentId) ?: run {
            transition.markFailed(
                command.taskId, FailureCategory.MISSING_REFERENCE, "target document not found: ${edge.targetDocumentId}",
            )
            return
        }

        publisher.publishEvent(ValidationTaskPreparedEvent(command.taskId, OffsetDateTime.now()))
    }

    @Recover
    fun recover(ex: Throwable, command: ProcessValidationTaskCommand) {
        // prep 단계는 AI 호출 이전이라 전송/응답 오류가 없다. 재시도 소진 후의 예외는
        // 내부 오류이므로 UNKNOWN으로 기록한다.
        transition.markFailed(command.taskId, FailureCategory.UNKNOWN, ex.message)
    }
}