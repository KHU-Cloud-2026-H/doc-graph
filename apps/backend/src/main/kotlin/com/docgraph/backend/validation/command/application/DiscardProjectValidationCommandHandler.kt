package com.docgraph.backend.validation.command.application

import com.docgraph.backend.validation.command.domain.AiUsageRecordRepository
import com.docgraph.backend.validation.command.domain.ConflictRepository
import com.docgraph.backend.validation.command.domain.ProjectValidationSettingRepository
import com.docgraph.backend.validation.command.domain.ValidationTaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class DiscardProjectValidationCommandHandler(
    private val taskRepository: ValidationTaskRepository,
    private val conflictRepository: ConflictRepository,
    private val aiUsageRecordRepository: AiUsageRecordRepository,
    private val settingRepository: ProjectValidationSettingRepository,
) {
    // ValidationProjectDiscardedEventListener(AFTER_COMMIT)에서 호출 — afterCommit 단계의 REQUIRED는
    // 커밋되지 않으므로 REQUIRES_NEW로 독립 트랜잭션을 연다. (graph DiscardGraphProjectCommandHandler와 동일)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(command: DiscardProjectValidationCommand) {
        // conflict_finding은 conflict·validation_task 양쪽 ON DELETE CASCADE라 함께 삭제된다.
        taskRepository.deleteByProjectId(command.projectId)
        conflictRepository.deleteByProjectId(command.projectId)
        aiUsageRecordRepository.deleteByProjectId(command.projectId)
        settingRepository.deleteByProjectId(command.projectId)
    }
}
