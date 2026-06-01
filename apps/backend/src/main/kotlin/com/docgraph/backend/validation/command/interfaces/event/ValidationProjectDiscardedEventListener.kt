package com.docgraph.backend.validation.command.interfaces.event

import com.docgraph.backend.project.command.domain.ProjectDiscardedEvent
import com.docgraph.backend.validation.command.application.DiscardProjectValidationCommand
import com.docgraph.backend.validation.command.application.DiscardProjectValidationCommandHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ValidationProjectDiscardedEventListener(
    private val handler: DiscardProjectValidationCommandHandler,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: ProjectDiscardedEvent) {
        handler.handle(DiscardProjectValidationCommand(event.projectId))
    }
}
