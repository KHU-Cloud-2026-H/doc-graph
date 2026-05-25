package com.docgraph.backend.notification.command.infra

import com.docgraph.backend.notification.command.domain.ProjectWebhook
import org.springframework.data.jpa.repository.JpaRepository

interface ProjectWebhookJpaRepository : JpaRepository<ProjectWebhook, Long> {
    fun findByProjectId(projectId: Long): ProjectWebhook?
    fun deleteByProjectId(projectId: Long)
}
