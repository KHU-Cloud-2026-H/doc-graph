package com.docgraph.backend.notification.command.infra

import com.docgraph.backend.notification.command.domain.ProjectWebhook
import com.docgraph.backend.notification.command.domain.ProjectWebhookRepository
import org.springframework.stereotype.Component

@Component
class ProjectWebhookRepositoryImpl(
    private val jpa: ProjectWebhookJpaRepository,
) : ProjectWebhookRepository {
    override fun save(projectWebhook: ProjectWebhook): ProjectWebhook = jpa.save(projectWebhook)
    override fun findByProjectId(projectId: Long): ProjectWebhook? = jpa.findByProjectId(projectId)
    override fun deleteByProjectId(projectId: Long) = jpa.deleteByProjectId(projectId)
}
