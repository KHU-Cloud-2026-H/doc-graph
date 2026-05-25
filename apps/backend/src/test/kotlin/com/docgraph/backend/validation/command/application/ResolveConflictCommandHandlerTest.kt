package com.docgraph.backend.validation.command.application

import com.docgraph.backend.validation.command.domain.Conflict
import com.docgraph.backend.validation.command.domain.ConflictRepository
import com.docgraph.backend.validation.command.domain.ConflictResolvedEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Tag("unit")
class ResolveConflictCommandHandlerTest {

    private val repository = mockk<ConflictRepository>()
    private val publisher = mockk<ApplicationEventPublisher>(relaxed = true)
    private val handler = ResolveConflictCommandHandler(repository, publisher)

    private val now = OffsetDateTime.parse("2026-05-01T00:00:00Z")
    private val resolvedAt = OffsetDateTime.parse("2026-05-10T00:00:00Z")

    @Test
    fun `active conflict — markResolved + ConflictResolvedEvent 발행`() {
        val conflict = Conflict(id = 42L, edgeId = 17L, firstDetectedAt = now, lastDetectedAt = now)
        every { repository.findById(42L) } returns conflict
        every { repository.save(any()) } returns conflict

        handler.handle(ResolveConflictCommand(conflictId = 42L, occurredAt = resolvedAt))

        assertNotNull(conflict.resolvedAt)
        assertEquals(resolvedAt, conflict.resolvedAt)
        verify(exactly = 1) {
            publisher.publishEvent(ConflictResolvedEvent(conflictId = 42L, edgeId = 17L, occurredAt = resolvedAt))
        }
    }

    @Test
    fun `이미 resolved — no-op idempotent`() {
        val conflict = Conflict(
            id = 42L, edgeId = 17L, firstDetectedAt = now, lastDetectedAt = now, resolvedAt = now,
        )
        every { repository.findById(42L) } returns conflict

        handler.handle(ResolveConflictCommand(conflictId = 42L, occurredAt = resolvedAt))

        verify(exactly = 0) { repository.save(any()) }
        verify(exactly = 0) { publisher.publishEvent(any<ConflictResolvedEvent>()) }
    }

    @Test
    fun `conflict 없음 — no-op`() {
        every { repository.findById(42L) } returns null

        handler.handle(ResolveConflictCommand(conflictId = 42L, occurredAt = resolvedAt))

        verify(exactly = 0) { repository.save(any()) }
        verify(exactly = 0) { publisher.publishEvent(any<ConflictResolvedEvent>()) }
    }
}
