package com.docgraph.backend.validation.command.application

import com.docgraph.backend.document.query.application.DocumentDetail
import com.docgraph.backend.document.query.application.DocumentType
import com.docgraph.backend.document.query.application.FindDocumentByIdQuery
import com.docgraph.backend.graph.query.application.EdgeDetail
import com.docgraph.backend.graph.query.application.FindEdgeByIdQuery
import com.docgraph.backend.validation.command.domain.Conflict
import com.docgraph.backend.validation.command.domain.ConflictFinding
import com.docgraph.backend.validation.command.domain.ConflictFindingNotFoundException
import com.docgraph.backend.validation.command.domain.ConflictFindingRepository
import com.docgraph.backend.validation.command.domain.ConflictRepository
import com.docgraph.backend.validation.command.domain.IllegalConflictFindingStateException
import com.docgraph.backend.validation.command.domain.ProposalApprovedEvent
import com.docgraph.backend.validation.command.domain.StaleProposalException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.ApplicationEventPublisher
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Tag("unit")
class ApproveProposalCommandHandlerTest {

    private val findingRepository = mockk<ConflictFindingRepository>(relaxed = true)
    private val conflictRepository = mockk<ConflictRepository>()
    private val findEdgeById = mockk<FindEdgeByIdQuery>()
    private val findDocumentById = mockk<FindDocumentByIdQuery>()
    private val publisher = mockk<ApplicationEventPublisher>(relaxed = true)

    private val handler = ApproveProposalCommandHandler(
        findingRepository = findingRepository,
        conflictRepository = conflictRepository,
        findEdgeById = findEdgeById,
        findDocumentById = findDocumentById,
        publisher = publisher,
    )

    private val targetEdited = OffsetDateTime.parse("2026-05-17T10:00:00Z")

    @Test
    fun `정상 — finding approve + ProposalApprovedEvent 발행`() {
        val finding = stubFinding(id = 50L, conflictId = 60L)
        wireConflictChain(conflictId = 60L, edgeId = 70L, targetDocumentId = 80L, targetEdited = targetEdited)
        every { findingRepository.findById(50L) } returns finding

        val captured = slot<ProposalApprovedEvent>()
        every { publisher.publishEvent(capture(captured)) } returns Unit

        handler.handle(
            ApproveProposalCommand(
                findingId = 50L,
                approvedBy = 99L,
                expectedTargetNotionLastEditedAt = targetEdited,
            ),
        )

        assertTrue(finding.isApproved)
        assertEquals(99L, finding.approvedBy)
        verify { publisher.publishEvent(any<ProposalApprovedEvent>()) }
        assertEquals(50L, captured.captured.conflictFindingId)
        assertEquals(99L, captured.captured.approvedBy)
    }

    @Test
    fun `finding 없음 — ConflictFindingNotFoundException`() {
        every { findingRepository.findById(404L) } returns null

        assertThrows<ConflictFindingNotFoundException> {
            handler.handle(
                ApproveProposalCommand(findingId = 404L, approvedBy = 1L, expectedTargetNotionLastEditedAt = targetEdited),
            )
        }
        verify(exactly = 0) { publisher.publishEvent(any<ProposalApprovedEvent>()) }
    }

    @Test
    fun `이미 승인됨 — IllegalConflictFindingStateException`() {
        val finding = stubFinding(id = 51L, conflictId = 60L).apply {
            approve(by = 1L, at = OffsetDateTime.now())
        }
        wireConflictChain(conflictId = 60L, edgeId = 70L, targetDocumentId = 80L, targetEdited = targetEdited)
        every { findingRepository.findById(51L) } returns finding

        assertThrows<IllegalConflictFindingStateException> {
            handler.handle(
                ApproveProposalCommand(findingId = 51L, approvedBy = 2L, expectedTargetNotionLastEditedAt = targetEdited),
            )
        }
        verify(exactly = 0) { publisher.publishEvent(any<ProposalApprovedEvent>()) }
    }

    @Test
    fun `target 문서가 사이에 변경됨 — StaleProposalException`() {
        val finding = stubFinding(id = 52L, conflictId = 60L)
        wireConflictChain(
            conflictId = 60L,
            edgeId = 70L,
            targetDocumentId = 80L,
            targetEdited = OffsetDateTime.parse("2026-05-17T11:30:00Z"),
        )
        every { findingRepository.findById(52L) } returns finding

        assertThrows<StaleProposalException> {
            handler.handle(
                ApproveProposalCommand(findingId = 52L, approvedBy = 1L, expectedTargetNotionLastEditedAt = targetEdited),
            )
        }
        verify(exactly = 0) { publisher.publishEvent(any<ProposalApprovedEvent>()) }
    }

    @Test
    fun `expected null + target도 null — 통과 (둘 다 미세팅)`() {
        val finding = stubFinding(id = 53L, conflictId = 60L)
        wireConflictChain(conflictId = 60L, edgeId = 70L, targetDocumentId = 80L, targetEdited = null)
        every { findingRepository.findById(53L) } returns finding
        every { publisher.publishEvent(any<ProposalApprovedEvent>()) } returns Unit

        handler.handle(
            ApproveProposalCommand(findingId = 53L, approvedBy = 1L, expectedTargetNotionLastEditedAt = null),
        )

        assertTrue(finding.isApproved)
    }

    private fun stubFinding(id: Long, conflictId: Long): ConflictFinding = ConflictFinding(
        id = id,
        conflictId = conflictId,
        validationTaskId = 1L,
        sourceBlockIds = listOf("s"),
        targetBlockId = "t",
        rationale = "r",
        newText = "sug",
        title = "tt",
        detectedAt = OffsetDateTime.now(),
    )

    private fun wireConflictChain(conflictId: Long, edgeId: Long, targetDocumentId: Long, targetEdited: OffsetDateTime?) {
        val conflict = Conflict(
            id = conflictId,
            edgeId = edgeId,
            firstDetectedAt = OffsetDateTime.now(),
            lastDetectedAt = OffsetDateTime.now(),
        )
        every { conflictRepository.findById(conflictId) } returns conflict
        every { findEdgeById.find(edgeId) } returns EdgeDetail(
            id = edgeId,
            projectId = 1L,
            sourceDocumentId = 100L,
            targetDocumentId = targetDocumentId,
            validationCriterion = "c",
        )
        every { findDocumentById.find(targetDocumentId) } returns DocumentDetail(
            id = targetDocumentId,
            notionPageId = "page",
            title = "title",
            type = DocumentType.REQUIREMENTS,
            parentDocumentId = null,
            icon = null,
            assigneeMemberId = null,
            notionLastEditedAt = targetEdited,
            blocks = emptyList(),
        )
    }
}
