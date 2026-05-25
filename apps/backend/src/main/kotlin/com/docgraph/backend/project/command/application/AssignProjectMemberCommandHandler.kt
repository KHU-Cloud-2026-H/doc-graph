package com.docgraph.backend.project.command.application

import com.docgraph.backend.project.command.domain.ProjectMember
import com.docgraph.backend.project.command.domain.ProjectMemberDuplicateException
import com.docgraph.backend.project.command.domain.ProjectMemberRepository
import com.docgraph.backend.project.command.domain.ProjectNotFoundException
import com.docgraph.backend.project.command.domain.ProjectRepository
import com.docgraph.backend.project.command.domain.ProjectWorkspaceMemberNotFoundException
import com.docgraph.backend.workspace.query.application.FindWorkspaceIdByMemberIdQuery
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AssignProjectMemberCommandHandler(
    private val projectRepository: ProjectRepository,
    private val projectMemberRepository: ProjectMemberRepository,
    private val authService: ProjectMemberAuthService,
    private val findWorkspaceIdByMemberId: FindWorkspaceIdByMemberIdQuery,
) {
    @Transactional
    fun handle(command: AssignProjectMemberCommand): Long {
        val project = projectRepository.findById(command.projectId)
            ?: throw ProjectNotFoundException(command.projectId)

        authService.requireAdminMember(project, command.requesterUserId)

        val targetWorkspaceId = findWorkspaceIdByMemberId.find(command.workspaceMemberId)
            ?: throw ProjectWorkspaceMemberNotFoundException(project.workspaceId, command.workspaceMemberId)
        if (targetWorkspaceId != project.workspaceId) {
            throw ProjectWorkspaceMemberNotFoundException(project.workspaceId, command.workspaceMemberId)
        }

        if (projectMemberRepository.findByProjectIdAndWorkspaceMemberId(project.id, command.workspaceMemberId) != null) {
            throw ProjectMemberDuplicateException(project.id, command.workspaceMemberId)
        }

        return projectMemberRepository.save(
            ProjectMember(
                projectId = project.id,
                workspaceMemberId = command.workspaceMemberId,
                role = command.role,
            ),
        ).id
    }
}
