package com.docgraph.backend.notification.command.domain

interface ProjectWebhookRepository {
    fun save(projectWebhook: ProjectWebhook): ProjectWebhook
    fun findByProjectId(projectId: Long): ProjectWebhook?
    fun deleteByProjectId(projectId: Long)
}