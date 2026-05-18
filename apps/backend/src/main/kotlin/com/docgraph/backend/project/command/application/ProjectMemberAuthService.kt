package com.docgraph.backend.project.command.application

import com.docgraph.backend.project.command.domain.Project
import com.docgraph.backend.project.command.domain.ProjectMember
import com.docgraph.backend.project.command.domain.ProjectMemberRepository
import com.docgraph.backend.project.command.domain.ProjectMemberRole
import com.docgraph.backend.project.command.domain.ProjectPermissionDeniedException
import com.docgraph.backend.workspace.query.application.FindWorkspaceMemberIdByUserIdQuery
import org.springframework.stereotype.Service

@Service
class ProjectMemberAuthService(
    private val projectMemberRepository: ProjectMemberRepository,
    private val findWorkspaceMemberId: FindWorkspaceMemberIdByUserIdQuery,
) {
    fun requireAdminMember(project: Project, requesterUserId: Long): ProjectMember {
        val wsMemberId = findWorkspaceMemberId.find(project.workspaceId, requesterUserId)
            ?: throw ProjectPermissionDeniedException(project.workspaceId, requesterUserId)
        val pmember = projectMemberRepository.findByProjectIdAndWorkspaceMemberId(project.id, wsMemberId)
            ?: throw ProjectPermissionDeniedException(project.workspaceId, requesterUserId)
        if (pmember.role != ProjectMemberRole.ADMIN) {
            throw ProjectPermissionDeniedException(project.workspaceId, requesterUserId)
        }
        return pmember
    }
}
