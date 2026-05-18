package com.docgraph.backend.workspace.command.application

import com.docgraph.backend.workspace.command.domain.Workspace
import com.docgraph.backend.workspace.command.domain.WorkspaceMember
import com.docgraph.backend.workspace.command.domain.WorkspaceMemberRepository
import com.docgraph.backend.workspace.command.domain.WorkspaceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegisterWorkspaceCommandHandler(
    private val workspaceRepository: WorkspaceRepository,
    private val workspaceMemberRepository: WorkspaceMemberRepository,
) {
    @Transactional
    fun handle(command: RegisterWorkspaceCommand): Long {
        val existing = workspaceRepository.findByNotionWorkspaceId(command.notionWorkspaceId)
        if (existing != null) {
            return existing.id
        }
        val workspace = workspaceRepository.save(
            Workspace(
                notionWorkspaceId = command.notionWorkspaceId,
                notionWorkspaceName = command.notionWorkspaceName,
                notionBotId = command.notionBotId,
                createdBy = command.ownerUserId,
            ),
        )
        workspaceMemberRepository.save(
            WorkspaceMember(workspaceId = workspace.id, userId = command.ownerUserId),
        )
        return workspace.id
    }
}
