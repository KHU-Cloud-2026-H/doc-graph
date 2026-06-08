package com.docgraph.backend.validation.command.interfaces.event

import com.docgraph.backend.document.query.application.FindDocumentByIdQuery
import com.docgraph.backend.event.OutboxStatus
import com.docgraph.backend.graph.query.application.FindEdgeByIdQuery
import com.docgraph.backend.validation.command.application.CompleteValidationTaskCommand
import com.docgraph.backend.validation.command.application.CompleteValidationTaskCommandHandler
import com.docgraph.backend.validation.command.application.RecordAiUsageCommand
import com.docgraph.backend.validation.command.application.RecordAiUsageCommandHandler
import com.docgraph.backend.validation.command.application.ValidationTaskTransitionService
import com.docgraph.backend.validation.command.domain.ConflictDetector
import com.docgraph.backend.validation.command.domain.FirstValidationInput
import com.docgraph.backend.validation.command.domain.RevalidationInput
import com.docgraph.backend.validation.command.domain.ValidationTaskPreparedEvent
import com.docgraph.backend.validation.command.domain.ValidationTaskRepository
import org.slf4j.LoggerFactory
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Recover
import org.springframework.retry.annotation.Retryable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException

@Component
class ValidationTaskPreparedEventListener(
    private val taskRepository: ValidationTaskRepository,
    private val findEdgeById: FindEdgeByIdQuery,
    private val findDocumentById: FindDocumentByIdQuery,
    private val detector: ConflictDetector,
    private val completeHandler: CompleteValidationTaskCommandHandler,
    private val recordAiUsageHandler: RecordAiUsageCommandHandler,
    private val transition: ValidationTaskTransitionService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(
        // transient 실패만 재시도: 429(rate limit)·5xx·연결/읽기 타임아웃.
        // 비-429 4xx(인증·잘못된 요청), 응답 파싱 실패, 조회 누락 등은 재호출해도
        // 결과가 같으므로 재시도하지 않고 Recover로 직행한다.
        retryFor = [
            HttpServerErrorException::class,
            HttpClientErrorException.TooManyRequests::class,
            ResourceAccessException::class,
        ],
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

        val input = if (source.blocks.any { it.previousText != null }) {
            RevalidationInput(
                sourceBeforeBlocks = source.blocks.map { it.copy(text = it.previousText) },
                sourceAfterBlocks = source.blocks,
                targetBlocks = target.blocks,
                criterion = edge.validationCriterion,
            )
        } else {
            FirstValidationInput(
                sourceBlocks = source.blocks,
                targetBlocks = target.blocks,
                criterion = edge.validationCriterion,
            )
        }
        val result = detector.detect(input)
        completeHandler.handle(CompleteValidationTaskCommand(task.id, result.conflicts))

        // 사용량 관측은 best-effort: 적재 실패가 이미 완료된 검출 결과를 롤백/실패시키면 안 된다.
        result.usage?.let { usage ->
            runCatching {
                recordAiUsageHandler.handle(RecordAiUsageCommand(task.id, edge.projectId, usage))
            }.onFailure { log.warn("AI 사용량 적재 실패 — task={}", task.id, it) }
        }
    }

    @Recover
    fun recover(ex: Throwable, event: ValidationTaskPreparedEvent) {
        val classification = ValidationFailureClassifier.classify(ex)
        log.error(
            "검증 태스크 실패 — 재시도 소진, 실패 기록: taskId={} classification={}: {}",
            event.validationTaskId, classification, ex.message, ex,
        )
        transition.markFailed(event.validationTaskId, classification, ex.message)
    }
}
