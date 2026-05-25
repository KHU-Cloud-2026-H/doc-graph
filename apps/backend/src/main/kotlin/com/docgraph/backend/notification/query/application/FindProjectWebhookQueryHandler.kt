package com.docgraph.backend.notification.query.application

import com.docgraph.backend.notification.command.domain.ProjectWebhookRepository
import com.docgraph.backend.project.query.application.SearchAdminProjectIdsByUserIdQuery
import org.springframework.stereotype.Service

@Service
class FindProjectWebhookQueryHandler(
    private val projectWebhookRepository: ProjectWebhookRepository,
    private val searchAdminProjectIds: SearchAdminProjectIdsByUserIdQuery,
) : FindProjectWebhookQuery {
    override fun find(projectId: Long, userId: Long): WebhookResponse? {
        if (projectId !in searchAdminProjectIds.search(userId)) return null
        return WebhookResponse(projectWebhookRepository.findByProjectId(projectId)?.url)
    }
}
