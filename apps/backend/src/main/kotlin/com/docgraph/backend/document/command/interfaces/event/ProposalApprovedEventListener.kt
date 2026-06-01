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
import org.slf4j.LoggerFactory
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
        val result = try {
            notionDocumentClient.patchBlockText(
                notionBlockId = finding.targetBlockId,
                newText = finding.newText,
                // lost-update 가드는 승인 시점에 ApproveProposalCommandHandler가 문서 단위로 처리(UC4) → writer엔 미전달.
                expectedLastEditedAt = null,
                accessToken = accessToken,
            )
        } catch (e: Exception) {
            // 권한·네트워크 오류 시 writer가 throw. @Async라 호출자에 전파 안 되므로 여기서 로그.
            // 이벤트 미발행 → Conflict 유지(UC4 실패 분기), 인박스에 남아 재승인 가능.
            logger.warn(
                "Notion 쓰기 실패 — finding={} block={} 이벤트 미발행(Conflict 유지): {}",
                finding.findingId, finding.targetBlockId, e.message,
            )
            return
        }
        if (result == NotionPatchResult.Success) {
            publisher.publishEvent(
                NotionWriteSucceededEvent(
                    conflictFindingId = finding.findingId,
                    occurredAt = event.occurredAt,
                ),
            )
        }
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

    companion object {
        private val logger = LoggerFactory.getLogger(ProposalApprovedEventListener::class.java)
    }
}
