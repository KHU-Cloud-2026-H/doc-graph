package com.docgraph.backend.project.command.application

import com.docgraph.backend.project.command.domain.ProjectDiscardedEvent
import com.docgraph.backend.project.command.domain.ProjectNotFoundException
import com.docgraph.backend.project.command.domain.ProjectRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DiscardProjectCommandHandler(
    private val projectRepository: ProjectRepository,
    private val authService: ProjectMemberAuthService,
    private val publisher: ApplicationEventPublisher,
) {
    @Transactional
    fun handle(command: DiscardProjectCommand) {
        val project = projectRepository.findById(command.projectId)
            ?: throw ProjectNotFoundException(command.projectId)

        authService.requireAdminMember(project, command.requesterUserId)

        projectRepository.delete(project)
        publisher.publishEvent(ProjectDiscardedEvent(projectId = project.id))
    }
}
