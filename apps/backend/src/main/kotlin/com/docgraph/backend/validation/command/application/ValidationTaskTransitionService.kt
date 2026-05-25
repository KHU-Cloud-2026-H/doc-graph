package com.docgraph.backend.validation.command.application

import com.docgraph.backend.validation.command.domain.ValidationTaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class ValidationTaskTransitionService(
    private val repository: ValidationTaskRepository,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun recordAttempt(taskId: Long) {
        val task = repository.findById(taskId) ?: error("validation task not found: $taskId")
        task.recordAttempt()
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun markFailed(taskId: Long, reason: String?) {
        val task = repository.findById(taskId) ?: error("validation task not found: $taskId")
        task.markFailed(reason)
    }
}