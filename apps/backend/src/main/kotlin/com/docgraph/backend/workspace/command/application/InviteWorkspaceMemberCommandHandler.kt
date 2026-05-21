package com.docgraph.backend.workspace.command.application

import com.docgraph.backend.auth.query.application.FindUserIdByEmailQuery
import com.docgraph.backend.workspace.command.domain.WorkspaceInviteeNotFoundException
import com.docgraph.backend.workspace.command.domain.WorkspaceMember
import com.docgraph.backend.workspace.command.domain.WorkspaceMemberDuplicateException
import com.docgraph.backend.workspace.command.domain.WorkspaceMemberRepository
import com.docgraph.backend.workspace.command.domain.WorkspaceNotFoundException
import com.docgraph.backend.workspace.command.domain.WorkspacePermissionDeniedException
import com.docgraph.backend.workspace.command.domain.WorkspaceRepository
import com.docgraph.backend.workspace.command.domain.WorkspaceSelfInviteException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InviteWorkspaceMemberCommandHandler(
    private val workspaceRepository: WorkspaceRepository,
    private val memberRepository: WorkspaceMemberRepository,
    private val findUserIdByEmail: FindUserIdByEmailQuery,
) {
    @Transactional
    fun handle(command: InviteWorkspaceMemberCommand): Long {
        val workspace = workspaceRepository.findById(command.workspaceId)
            ?: throw WorkspaceNotFoundException(command.workspaceId)

        if (workspace.createdBy != command.requesterUserId) {
            throw WorkspacePermissionDeniedException(command.workspaceId, command.requesterUserId)
        }

        val inviteeMemberId = findUserIdByEmail.find(command.inviteeEmail)
            ?: throw WorkspaceInviteeNotFoundException(command.inviteeEmail)

        if (inviteeMemberId == workspace.createdBy) {
            throw WorkspaceSelfInviteException(command.workspaceId)
        }

        if (memberRepository.findByWorkspaceIdAndUserId(command.workspaceId, inviteeMemberId) != null) {
            throw WorkspaceMemberDuplicateException(command.workspaceId, inviteeMemberId)
        }

        val member = WorkspaceMember(
            workspaceId = command.workspaceId,
            userId = inviteeMemberId,
        )
        return memberRepository.save(member).id
    }
}
