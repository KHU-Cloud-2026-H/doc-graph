package com.docgraph.backend.document.command.interfaces.event

import com.docgraph.backend.auth.command.domain.NotionConnection
import com.docgraph.backend.auth.command.domain.NotionConnectionRepository
import com.docgraph.backend.auth.command.infra.NotionAccessTokenDecryptor
import com.docgraph.backend.document.command.domain.Document
import com.docgraph.backend.document.command.domain.DocumentRepository
import com.docgraph.backend.document.command.domain.NotionDocumentClient
import com.docgraph.backend.document.command.domain.NotionPatchResult
import com.docgraph.backend.document.command.domain.NotionWriteSucceededEvent
import com.docgraph.backend.project.command.domain.Project
import com.docgraph.backend.project.command.domain.ProjectRepository
import com.docgraph.backend.validation.command.domain.ProposalApprovedEvent
import com.docgraph.backend.validation.query.application.ConflictFindingDetail
import com.docgraph.backend.validation.query.application.FindConflictFindingByIdQuery
import com.docgraph.backend.workspace.command.domain.Workspace
import com.docgraph.backend.workspace.command.domain.WorkspaceRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.time.OffsetDateTime
import java.util.Optional

@Tag("unit")
class ProposalApprovedEventListenerTest {

    private val findConflictFindingById = mockk<FindConflictFindingByIdQuery>()
    private val documentRepository = mockk<DocumentRepository>()
    private val projectRepository = mockk<ProjectRepository>()
    private val workspaceRepository = mockk<WorkspaceRepository>()
    private val notionConnectionRepository = mockk<NotionConnectionRepository>()
    private val notionAccessTokenDecryptor = mockk<NotionAccessTokenDecryptor>()
    private val notionDocumentClient = mockk<NotionDocumentClient>()
    private val publisher = mockk<ApplicationEventPublisher>(relaxed = true)

    private val listener = ProposalApprovedEventListener(
        findConflictFindingById,
        documentRepository,
        projectRepository,
        workspaceRepository,
        notionConnectionRepository,
        notionAccessTokenDecryptor,
        notionDocumentClient,
        publisher,
    )

    private val now = OffsetDateTime.parse("2026-05-01T00:00:00Z")

    @Test
    fun `Success — finding 조회 + 토큰 해석 + Notion patch + NotionWriteSucceededEvent 발행`() {
        every { findConflictFindingById.find(500L) } returns ConflictFindingDetail(
            findingId = 500L,
            targetDocumentId = 20L,
            targetBlockId = "block-x",
            newText = "new",
        )
        stubTokenChain(documentId = 20L, approvedBy = 1L, token = "token-1")
        every { notionDocumentClient.patchBlockText("block-x", "new", null, "token-1") } returns NotionPatchResult.Success

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

    @Test
    fun `Notion 쓰기 실패(throw) — 이벤트 미발행, Conflict 유지`() {
        every { findConflictFindingById.find(500L) } returns ConflictFindingDetail(
            findingId = 500L,
            targetDocumentId = 20L,
            targetBlockId = "block-x",
            newText = "new",
        )
        stubTokenChain(documentId = 20L, approvedBy = 1L, token = "token-1")
        every { notionDocumentClient.patchBlockText("block-x", "new", null, "token-1") } throws
            IllegalStateException("Notion 쓰기 권한 오류")

        listener.on(ProposalApprovedEvent(conflictFindingId = 500L, approvedBy = 1L, occurredAt = now))

        verify(exactly = 0) { publisher.publishEvent(any<NotionWriteSucceededEvent>()) }
    }

    // 리스너의 resolveAccessToken: document → project → workspace → connection → decrypt 경로를 stub.
    private fun stubTokenChain(documentId: Long, approvedBy: Long, token: String) {
        val document = mockk<Document> { every { projectId } returns 30L }
        val project = mockk<Project> { every { workspaceId } returns 40L }
        val workspace = mockk<Workspace> { every { notionWorkspaceId } returns "nw-1" }
        val connection = mockk<NotionConnection> {
            every { revokedAt } returns null
            every { accessTokenEncrypted } returns "enc"
        }
        every { documentRepository.findById(documentId) } returns Optional.of(document)
        every { projectRepository.findById(30L) } returns project
        every { workspaceRepository.findById(40L) } returns workspace
        every { notionConnectionRepository.findByUserIdAndNotionWorkspaceId(approvedBy, "nw-1") } returns connection
        every { notionAccessTokenDecryptor.decrypt("enc") } returns token
    }
}
