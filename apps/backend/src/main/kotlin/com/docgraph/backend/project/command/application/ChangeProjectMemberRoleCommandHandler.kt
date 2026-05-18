package com.docgraph.backend.project.command.application

import com.docgraph.backend.project.command.domain.ProjectMemberNotFoundException
import com.docgraph.backend.project.command.domain.ProjectMemberRepository
import com.docgraph.backend.project.command.domain.ProjectNotFoundException
import com.docgraph.backend.project.command.domain.ProjectRepository
import com.docgraph.backend.project.command.domain.ProjectSelfMemberModificationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChangeProjectMemberRoleCommandHandler(
    private val projectRepository: ProjectRepository,
    private val projectMemberRepository: ProjectMemberRepository,
    private val authService: ProjectMemberAuthService,
) {
    @Transactional
    fun handle(command: ChangeProjectMemberRoleCommand) {
        val project = projectRepository.findById(command.projectId)
            ?: throw ProjectNotFoundException(command.projectId)

        val requester = authService.requireAdminMember(project, command.requesterUserId)

        val target = projectMemberRepository.findById(command.memberId)
            ?: throw ProjectMemberNotFoundException(command.projectId, command.memberId)
        if (target.projectId != command.projectId) {
            throw ProjectMemberNotFoundException(command.projectId, command.memberId)
        }
        if (target.id == requester.id) {
            throw ProjectSelfMemberModificationException(command.projectId, command.memberId)
        }

        target.role = command.role
        projectMemberRepository.save(target)
    }
}
