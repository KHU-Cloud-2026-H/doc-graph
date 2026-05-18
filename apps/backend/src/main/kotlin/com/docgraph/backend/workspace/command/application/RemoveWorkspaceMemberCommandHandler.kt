package com.docgraph.backend.workspace.command.application

import com.docgraph.backend.workspace.command.domain.WorkspaceCreatorRemovalException
import com.docgraph.backend.workspace.command.domain.WorkspaceMemberNotFoundException
import com.docgraph.backend.workspace.command.domain.WorkspaceMemberRepository
import com.docgraph.backend.workspace.command.domain.WorkspaceNotFoundException
import com.docgraph.backend.workspace.command.domain.WorkspacePermissionDeniedException
import com.docgraph.backend.workspace.command.domain.WorkspaceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RemoveWorkspaceMemberCommandHandler(
    private val workspaceRepository: WorkspaceRepository,
    private val memberRepository: WorkspaceMemberRepository,
) {
    @Transactional
    fun handle(command: RemoveWorkspaceMemberCommand) {
        val workspace = workspaceRepository.findById(command.workspaceId)
            ?: throw WorkspaceNotFoundException(command.workspaceId)

        if (workspace.createdBy != command.requesterUserId) {
            throw WorkspacePermissionDeniedException(command.workspaceId, command.requesterUserId)
        }

        val member = memberRepository.findById(command.memberId)
            ?: throw WorkspaceMemberNotFoundException(command.workspaceId, command.memberId)
        if (member.workspaceId != command.workspaceId) {
            throw WorkspaceMemberNotFoundException(command.workspaceId, command.memberId)
        }
        if (member.userId == workspace.createdBy) {
            throw WorkspaceCreatorRemovalException(command.workspaceId)
        }

        memberRepository.delete(member)
    }
}
