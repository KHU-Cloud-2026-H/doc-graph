package com.docgraph.backend.project.command.application

import com.docgraph.backend.project.command.domain.ProjectNotFoundException
import com.docgraph.backend.project.command.domain.ProjectRepository
import com.docgraph.backend.project.command.domain.ProjectSyncTriggeredEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TriggerProjectSyncCommandHandler(
    private val projectRepository: ProjectRepository,
    private val authService: ProjectMemberAuthService,
    private val publisher: ApplicationEventPublisher,
) {
    @Transactional
    fun handle(command: TriggerProjectSyncCommand) {
        val project = projectRepository.findById(command.projectId)
            ?: throw ProjectNotFoundException(command.projectId)

        authService.requireAdminMember(project, command.requesterUserId)

        publisher.publishEvent(
            ProjectSyncTriggeredEvent(
                projectId = project.id,
                triggeredBy = command.requesterUserId,
            ),
        )
    }
}
