package com.docgraph.backend.notification.command.interfaces.event

import com.docgraph.backend.notification.command.application.DiscardProjectWebhookCommand
import com.docgraph.backend.notification.command.application.DiscardProjectWebhookCommandHandler
import com.docgraph.backend.project.command.domain.ProjectDiscardedEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class NotificationProjectDiscardedEventListener(
    private val handler: DiscardProjectWebhookCommandHandler,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: ProjectDiscardedEvent) {
        handler.handle(DiscardProjectWebhookCommand(event.projectId))
    }
}
