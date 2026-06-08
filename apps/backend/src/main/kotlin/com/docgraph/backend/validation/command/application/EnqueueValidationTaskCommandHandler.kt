package com.docgraph.backend.validation.command.application

import com.docgraph.backend.graph.query.application.FindEdgeByIdQuery
import com.docgraph.backend.validation.command.domain.ProjectValidationSettingRepository
import com.docgraph.backend.validation.command.domain.ValidationTask
import com.docgraph.backend.validation.command.domain.ValidationTaskQueuedEvent
import com.docgraph.backend.validation.command.domain.ValidationTaskRepository
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class EnqueueValidationTaskCommandHandler(
    private val repository: ValidationTaskRepository,
    private val settingRepository: ProjectValidationSettingRepository,
    private val findEdgeById: FindEdgeByIdQuery,
    private val publisher: ApplicationEventPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(command: EnqueueValidationTaskCommand) {
        repository.findByValidationPairId(command.validationPairId)?.let {
            log.debug("validation task enqueue skipped (already queued): validationPairId={}", command.validationPairId)
            return
        }

        // 프로젝트 검증이 OFF면 enqueue하지 않는다(실제 AI 호출 0). edge를 못 정하면(race)
        // fail-open으로 기존 동작 유지 — process handler가 미해소 edge를 markFailed 처리.
        val edge = findEdgeById.find(command.edgeId)
        if (edge != null && settingRepository.findByProjectId(edge.projectId)?.enabled == false) {
            log.info("validation task enqueue skipped (validation disabled): edgeId={} projectId={}", command.edgeId, edge.projectId)
            return
        }

        val task = repository.save(
            ValidationTask(
                validationPairId = command.validationPairId,
                edgeId = command.edgeId,
                projectId = edge?.projectId,
            ),
        )
        publisher.publishEvent(ValidationTaskQueuedEvent(task.id))
        log.info("validation task queued: taskId={} edgeId={} projectId={}", task.id, command.edgeId, edge?.projectId)
    }
}