package com.docgraph.backend.notification.command.application

import com.docgraph.backend.notification.command.domain.NotificationPermissionDeniedException
import com.docgraph.backend.notification.command.domain.ProjectWebhook
import com.docgraph.backend.notification.command.domain.ProjectWebhookRepository
import com.docgraph.backend.project.query.application.SearchAdminProjectIdsByUserIdQuery
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class ConfigureProjectWebhookCommandHandler(
    private val projectWebhookRepository: ProjectWebhookRepository,
    private val searchAdminProjectIds: SearchAdminProjectIdsByUserIdQuery,
) {
    @Transactional
    fun handle(command: ConfigureProjectWebhookCommand) {
        if (command.projectId !in searchAdminProjectIds.search(command.requesterUserId)) {
            throw NotificationPermissionDeniedException(command.projectId)
        }
        val now = OffsetDateTime.now()
        val existing = projectWebhookRepository.findByProjectId(command.projectId)
        if (existing != null) {
            existing.changeUrl(command.url, now)
            projectWebhookRepository.save(existing)
        } else {
            projectWebhookRepository.save(
                ProjectWebhook(projectId = command.projectId, url = command.url, createdAt = now),
            )
        }
    }
}
