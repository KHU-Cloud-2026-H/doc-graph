package com.docgraph.backend.notification.command.interfaces.event

import com.docgraph.backend.notification.command.application.ConflictNotificationDispatcher
import com.docgraph.backend.validation.command.domain.ConflictDetectedEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class NotificationConflictDetectedEventListener(
    private val dispatcher: ConflictNotificationDispatcher,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: ConflictDetectedEvent) {
        dispatcher.dispatch(conflictId = event.conflictId, edgeId = event.edgeId)
    }
}
