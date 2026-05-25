package com.docgraph.backend.notification.command.application

import com.docgraph.backend.notification.command.domain.ProjectWebhookRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DiscardProjectWebhookCommandHandler(
    private val projectWebhookRepository: ProjectWebhookRepository,
) {
    @Transactional
    fun handle(command: DiscardProjectWebhookCommand) {
        projectWebhookRepository.deleteByProjectId(command.projectId)
    }
}
