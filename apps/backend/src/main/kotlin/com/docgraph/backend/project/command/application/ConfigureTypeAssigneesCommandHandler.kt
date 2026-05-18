package com.docgraph.backend.project.command.application

import com.docgraph.backend.project.command.domain.ProjectNotFoundException
import com.docgraph.backend.project.command.domain.ProjectRepository
import com.docgraph.backend.project.command.domain.ProjectWorkspaceMemberNotFoundException
import com.docgraph.backend.project.command.domain.TypeAssigneeDefault
import com.docgraph.backend.project.command.domain.TypeAssigneeDefaultRepository
import com.docgraph.backend.workspace.query.application.FindWorkspaceIdByMemberIdQuery
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ConfigureTypeAssigneesCommandHandler(
    private val projectRepository: ProjectRepository,
    private val typeAssigneeRepository: TypeAssigneeDefaultRepository,
    private val authService: ProjectMemberAuthService,
    private val findWorkspaceIdByMemberId: FindWorkspaceIdByMemberIdQuery,
) {
    @Transactional
    fun handle(command: ConfigureTypeAssigneesCommand) {
        val project = projectRepository.findById(command.projectId)
            ?: throw ProjectNotFoundException(command.projectId)

        authService.requireAdminMember(project, command.requesterUserId)

        for (assignment in command.assignments) {
            val memberId = assignment.assigneeWorkspaceMemberId ?: continue
            val targetWorkspaceId = findWorkspaceIdByMemberId.find(memberId)
                ?: throw ProjectWorkspaceMemberNotFoundException(project.workspaceId, memberId)
            if (targetWorkspaceId != project.workspaceId) {
                throw ProjectWorkspaceMemberNotFoundException(project.workspaceId, memberId)
            }
        }

        typeAssigneeRepository.deleteAllByProjectId(project.id)
        typeAssigneeRepository.saveAll(
            command.assignments.map {
                TypeAssigneeDefault(
                    projectId = project.id,
                    documentType = it.documentType,
                    assigneeWorkspaceMemberId = it.assigneeWorkspaceMemberId,
                )
            },
        )
    }
}
