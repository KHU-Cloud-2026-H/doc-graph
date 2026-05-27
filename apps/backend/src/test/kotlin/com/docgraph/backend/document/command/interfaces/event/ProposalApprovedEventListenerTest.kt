package com.docgraph.backend.document.command.interfaces.event

import com.docgraph.backend.document.command.domain.NotionDocumentClient
import com.docgraph.backend.document.command.domain.NotionPatchResult
import com.docgraph.backend.document.command.domain.NotionWriteSucceededEvent
import com.docgraph.backend.validation.command.domain.ProposalApprovedEvent
import com.docgraph.backend.validation.query.application.ConflictFindingDetail
import com.docgraph.backend.validation.query.application.FindConflictFindingByIdQuery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.time.OffsetDateTime

@Tag("unit")
class ProposalApprovedEventListenerTest {

    private val findConflictFindingById = mockk<FindConflictFindingByIdQuery>()
    private val notionDocumentClient = mockk<NotionDocumentClient>()
    private val publisher = mockk<ApplicationEventPublisher>(relaxed = true)

    private val listener = ProposalApprovedEventListener(findConflictFindingById, notionDocumentClient, publisher)

    private val now = OffsetDateTime.parse("2026-05-01T00:00:00Z")

    @Test
    fun `Success — finding 조회 + Notion patch + NotionWriteSucceededEvent 발행`() {
        every { findConflictFindingById.find(500L) } returns ConflictFindingDetail(
            findingId = 500L,
            targetDocumentId = 20L,
            targetBlockId = "block-x",
            newText = "new",
        )
        every { notionDocumentClient.patchBlockText("block-x", "new", null, null) } returns NotionPatchResult.Success

        listener.on(ProposalApprovedEvent(conflictFindingId = 500L, approvedBy = 1L, occurredAt = now))

        verify(exactly = 1) {
            publisher.publishEvent(NotionWriteSucceededEvent(conflictFindingId = 500L, occurredAt = now))
        }
    }

    @Test
    fun `finding 조회 null — Notion 호출 없이 no-op`() {
        every { findConflictFindingById.find(500L) } returns null

        listener.on(ProposalApprovedEvent(conflictFindingId = 500L, approvedBy = 1L, occurredAt = now))

        verify(exactly = 0) { notionDocumentClient.patchBlockText(any(), any(), any(), any()) }
        verify(exactly = 0) { publisher.publishEvent(any<NotionWriteSucceededEvent>()) }
    }
}
