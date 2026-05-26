package com.docgraph.backend.notification.command.application

import com.docgraph.backend.document.query.application.SearchDocumentReferencesByIdsQuery
import com.docgraph.backend.graph.query.application.FindEdgeByIdQuery
import com.docgraph.backend.notification.command.domain.ProjectWebhookRepository
import com.docgraph.backend.notification.command.domain.WebhookNotifier
import com.docgraph.backend.project.query.application.SearchProjectRefsByIdsQuery
import com.docgraph.backend.validation.query.application.FindConflictTitleByIdQuery
import org.springframework.stereotype.Service

/**
 * ConflictDetectedEvent 수신 시 프로젝트 webhook으로 알림을 조립·발송한다.
 * webhook 미설정 프로젝트면 cross-domain 조회 없이 종료. 영속 상태 변경이 없는 외부 부수효과이므로 application service.
 */
@Service
class ConflictNotificationDispatcher(
    private val projectWebhookRepository: ProjectWebhookRepository,
    private val findEdgeById: FindEdgeByIdQuery,
    private val searchProjectRefs: SearchProjectRefsByIdsQuery,
    private val searchDocumentReferences: SearchDocumentReferencesByIdsQuery,
    private val findConflictTitle: FindConflictTitleByIdQuery,
    private val webhookNotifier: WebhookNotifier,
) {
    fun dispatch(conflictId: Long, edgeId: Long) {
        val edge = findEdgeById.find(edgeId) ?: return
        val webhook = projectWebhookRepository.findByProjectId(edge.projectId) ?: return

        val projectName = searchProjectRefs.search(listOf(edge.projectId)).firstOrNull()?.name.orEmpty()
        val docs = searchDocumentReferences.search(listOf(edge.sourceDocumentId, edge.targetDocumentId))
            .associateBy { it.id }
        val sourceTitle = docs[edge.sourceDocumentId]?.title.orEmpty()
        val targetTitle = docs[edge.targetDocumentId]?.title.orEmpty()
        val title = findConflictTitle.find(conflictId)

        webhookNotifier.send(webhook.url, buildMessage(projectName, sourceTitle, targetTitle, title))
    }

    private fun buildMessage(projectName: String, sourceTitle: String, targetTitle: String, title: String?): String =
        buildString {
            append("⚠️ [$projectName] 정합성 충돌 감지")
            if (!title.isNullOrBlank()) append("\n$title")
            append("\n$sourceTitle → $targetTitle")
        }
}
