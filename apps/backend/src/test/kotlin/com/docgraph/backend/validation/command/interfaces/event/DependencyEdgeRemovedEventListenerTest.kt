package com.docgraph.backend.validation.command.interfaces.event

import com.docgraph.backend.graph.command.domain.DependencyEdgeRemovedEvent
import com.docgraph.backend.validation.command.application.ResolveConflictCommand
import com.docgraph.backend.validation.command.application.ResolveConflictCommandHandler
import com.docgraph.backend.validation.command.domain.Conflict
import com.docgraph.backend.validation.command.domain.ConflictRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

@Tag("unit")
class DependencyEdgeRemovedEventListenerTest {

    private val conflictRepository = mockk<ConflictRepository>()
    private val handler = mockk<ResolveConflictCommandHandler>(relaxed = true)
    private val listener = DependencyEdgeRemovedEventListener(conflictRepository, handler)

    private val now = OffsetDateTime.parse("2026-05-01T00:00:00Z")

    @Test
    fun `활성 Conflict 있음 — ResolveConflictCommand 호출`() {
        val conflict = Conflict(id = 42L, edgeId = 17L, firstDetectedAt = now, lastDetectedAt = now)
        every { conflictRepository.findFirstByEdgeIdAndResolvedAtIsNull(17L) } returns conflict

        listener.on(DependencyEdgeRemovedEvent(edgeId = 17L, projectId = 1L, occurredAt = now))

        verify(exactly = 1) { handler.handle(ResolveConflictCommand(conflictId = 42L, occurredAt = now)) }
    }

    @Test
    fun `활성 Conflict 없음 — no-op`() {
        every { conflictRepository.findFirstByEdgeIdAndResolvedAtIsNull(17L) } returns null

        listener.on(DependencyEdgeRemovedEvent(edgeId = 17L, projectId = 1L, occurredAt = now))

        verify(exactly = 0) { handler.handle(any()) }
    }
}
