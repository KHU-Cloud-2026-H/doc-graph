package com.docgraph.backend.workspace.command.application

import com.docgraph.backend.workspace.command.domain.Workspace
import com.docgraph.backend.workspace.command.domain.WorkspaceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegisterWorkspaceCommandHandler(
    private val workspaceRepository: WorkspaceRepository,
) {
    @Transactional
    fun handle(command: RegisterWorkspaceCommand): Long {
        val existing = workspaceRepository.findByNotionWorkspaceId(command.notionWorkspaceId)
        if (existing != null) {
            return existing.id
        }
        val workspace = Workspace(
            notionWorkspaceId = command.notionWorkspaceId,
            notionWorkspaceName = command.notionWorkspaceName,
            notionBotId = command.notionBotId,
            createdBy = command.ownerUserId,
        )
        return workspaceRepository.save(workspace).id
    }
}
