package com.docgraph.backend.validation.command.interfaces.event

import com.docgraph.backend.document.command.domain.NotionWriteSucceededEvent
import com.docgraph.backend.validation.command.application.ResolveConflictCommand
import com.docgraph.backend.validation.command.application.ResolveConflictCommandHandler
import com.docgraph.backend.validation.command.domain.ConflictFinding
import com.docgraph.backend.validation.command.domain.ConflictFindingRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

@Tag("unit")
class NotionWriteSucceededEventListenerTest {

    private val findingRepository = mockk<ConflictFindingRepository>()
    private val handler = mockk<ResolveConflictCommandHandler>(relaxed = true)
    private val listener = NotionWriteSucceededEventListener(findingRepository, handler)

    private val now = OffsetDateTime.parse("2026-05-01T00:00:00Z")

    @Test
    fun `finding 조회 + conflictId 변환 + ResolveConflictCommand 발행`() {
        every { findingRepository.findById(500L) } returns ConflictFinding(
            id = 500L,
            conflictId = 42L,
            validationTaskId = 1L,
            sourceBlockIds = listOf("s"),
            targetBlockId = "t",
            rationale = "r",
            newText = "n",
            title = "tt",
            detectedAt = now,
        )

        listener.on(NotionWriteSucceededEvent(conflictFindingId = 500L, occurredAt = now))

        verify(exactly = 1) { handler.handle(ResolveConflictCommand(conflictId = 42L, occurredAt = now)) }
    }

    @Test
    fun `finding null — handler 호출 안 함`() {
        every { findingRepository.findById(500L) } returns null

        listener.on(NotionWriteSucceededEvent(conflictFindingId = 500L, occurredAt = now))

        verify(exactly = 0) { handler.handle(any()) }
    }
}
