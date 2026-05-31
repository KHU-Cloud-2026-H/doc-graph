package com.docgraph.backend.document.command.interfaces.event

import com.docgraph.backend.auth.command.domain.NotionConnectionRepository
import com.docgraph.backend.auth.command.infra.NotionAccessTokenDecryptor
import com.docgraph.backend.document.command.domain.DocumentRepository
import com.docgraph.backend.document.command.domain.NotionDocumentClient
import com.docgraph.backend.document.command.domain.NotionPatchResult
import com.docgraph.backend.document.command.domain.NotionWriteSucceededEvent
import com.docgraph.backend.project.command.domain.ProjectRepository
import com.docgraph.backend.validation.command.domain.ProposalApprovedEvent
import com.docgraph.backend.validation.query.application.FindConflictFindingByIdQuery
import com.docgraph.backend.workspace.command.domain.WorkspaceRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ProposalApprovedEventListener(
    private val findConflictFindingById: FindConflictFindingByIdQuery,
    private val documentRepository: DocumentRepository,
    private val projectRepository: ProjectRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val notionConnectionRepository: NotionConnectionRepository,
    private val notionAccessTokenDecryptor: NotionAccessTokenDecryptor,
    private val notionDocumentClient: NotionDocumentClient,
    private val publisher: ApplicationEventPublisher,
) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: ProposalApprovedEvent) {
        val finding = findConflictFindingById.find(event.conflictFindingId) ?: return
        val accessToken = resolveAccessToken(finding.targetDocumentId, event.approvedBy) ?: return
        val result = notionDocumentClient.patchBlockText(
            notionBlockId = finding.targetBlockId,
            newText = finding.newText,
            // TODO: ConflictFinding에 targetBlockLastEditedAt 저장 후 전달 — 검출 이후 수정된 block 덮어쓰기(lost-update) 방지
            expectedLastEditedAt = null,
            accessToken = accessToken,
        )
        if (result == NotionPatchResult.Success) {
            publisher.publishEvent(
                NotionWriteSucceededEvent(
                    conflictFindingId = finding.findingId,
                    occurredAt = event.occurredAt,
                ),
            )
        }
        // TODO: PATCH 실패(401·네트워크·버전 충돌) 시 재시도/실패 통지 경로 추가 — finding 승인 후 Notion 미반영 상태 방지
    }

    private fun resolveAccessToken(documentId: Long, userId: Long): String? {
        val document = documentRepository.findByIdOrNull(documentId) ?: return null
        val project = projectRepository.findById(document.projectId) ?: return null
        val workspace = workspaceRepository.findById(project.workspaceId) ?: return null
        val connection = notionConnectionRepository.findByUserIdAndNotionWorkspaceId(
            userId = userId,
            notionWorkspaceId = workspace.notionWorkspaceId,
        ) ?: return null
        if (connection.revokedAt != null) return null
        return notionAccessTokenDecryptor.decrypt(connection.accessTokenEncrypted)
    }
}
