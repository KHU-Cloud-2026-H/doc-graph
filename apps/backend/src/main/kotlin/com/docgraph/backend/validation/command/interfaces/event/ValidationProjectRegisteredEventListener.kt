package com.docgraph.backend.validation.command.interfaces.event

import com.docgraph.backend.project.command.domain.ProjectRegisteredEvent
import com.docgraph.backend.validation.command.application.InitializeProjectValidationCommand
import com.docgraph.backend.validation.command.application.InitializeProjectValidationCommandHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ValidationProjectRegisteredEventListener(
    private val handler: InitializeProjectValidationCommandHandler,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: ProjectRegisteredEvent) {
        // 기본 ON(row 부재=활성)이므로 disabled로 생성된 경우에만 setting을 영속한다.
        if (!event.validationEnabled) {
            handler.handle(InitializeProjectValidationCommand(projectId = event.projectId, enabled = false))
        }
    }
}
