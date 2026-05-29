package com.docgraph.backend.notification.command.interfaces.event

import com.docgraph.backend.notification.command.application.ConflictNotificationDispatcher
import com.docgraph.backend.validation.command.domain.ConflictDetectedEvent
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

@Tag("unit")
class NotificationConflictDetectedEventListenerTest {

    private val dispatcher = mockk<ConflictNotificationDispatcher>()
    private val listener = NotificationConflictDetectedEventListener(dispatcher)

    @Test
    fun `이벤트 수신 — conflictId·edgeId로 dispatch 위임`() {
        every { dispatcher.dispatch(conflictId = 10L, edgeId = 70L) } just Runs

        listener.on(ConflictDetectedEvent(conflictId = 10L, edgeId = 70L, occurredAt = OffsetDateTime.now()))

        verify(exactly = 1) { dispatcher.dispatch(conflictId = 10L, edgeId = 70L) }
    }
}
