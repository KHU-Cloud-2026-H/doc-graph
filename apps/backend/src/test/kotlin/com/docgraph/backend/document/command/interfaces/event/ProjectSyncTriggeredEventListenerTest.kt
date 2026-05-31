package com.docgraph.backend.document.command.interfaces.event

import com.docgraph.backend.document.command.application.SyncProjectDocumentsCommand
import com.docgraph.backend.document.command.application.SyncProjectDocumentsCommandHandler
import com.docgraph.backend.project.command.domain.ProjectSyncTriggeredEvent
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("unit")
class ProjectSyncTriggeredEventListenerTest {

    private val handler = mockk<SyncProjectDocumentsCommandHandler>(relaxed = true)
    private val listener = ProjectSyncTriggeredEventListener(handler)

    @Test
    fun `ProjectSyncTriggeredEvent 이후 document sync worker를 실행한다`() {
        listener.on(ProjectSyncTriggeredEvent(projectId = 10L, triggeredBy = 20L))

        verify(exactly = 1) {
            handler.handle(SyncProjectDocumentsCommand(projectId = 10L, requestedBy = 20L))
        }
    }
}
