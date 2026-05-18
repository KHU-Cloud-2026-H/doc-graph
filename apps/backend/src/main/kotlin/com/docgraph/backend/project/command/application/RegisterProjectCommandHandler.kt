package com.docgraph.backend.project.command.application

import com.docgraph.backend.project.command.domain.Project
import com.docgraph.backend.project.command.domain.ProjectMember
import com.docgraph.backend.project.command.domain.ProjectMemberRepository
import com.docgraph.backend.project.command.domain.ProjectMemberRole
import com.docgraph.backend.project.command.domain.ProjectPermissionDeniedException
import com.docgraph.backend.project.command.domain.ProjectRepository
import com.docgraph.backend.project.command.domain.ProjectWorkspaceNotFoundException
import com.docgraph.backend.workspace.query.application.FindWorkspaceCreatorIdByIdQuery
import com.docgraph.backend.workspace.query.application.FindWorkspaceMemberIdByUserIdQuery
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegisterProjectCommandHandler(
    private val projectRepository: ProjectRepository,
    private val projectMemberRepository: ProjectMemberRepository,
    private val findWorkspaceCreatorId: FindWorkspaceCreatorIdByIdQuery,
    private val findWorkspaceMemberId: FindWorkspaceMemberIdByUserIdQuery,
) {
    @Transactional
    fun handle(command: RegisterProjectCommand): Long {
        val creatorId = findWorkspaceCreatorId.find(command.workspaceId)
            ?: throw ProjectWorkspaceNotFoundException(command.workspaceId)

        if (creatorId != command.requesterUserId) {
            throw ProjectPermissionDeniedException(command.workspaceId, command.requesterUserId)
        }

        val workspaceMemberId = findWorkspaceMemberId.find(command.workspaceId, command.requesterUserId)
            ?: throw ProjectWorkspaceNotFoundException(command.workspaceId)

        val project = projectRepository.save(
            Project(
                workspaceId = command.workspaceId,
                name = command.name,
                notionRootPageId = command.notionRootPageId,
                createdBy = command.requesterUserId,
            ),
        )
        projectMemberRepository.save(
            ProjectMember(
                projectId = project.id,
                workspaceMemberId = workspaceMemberId,
                role = ProjectMemberRole.ADMIN,
            ),
        )
        return project.id
    }
}
