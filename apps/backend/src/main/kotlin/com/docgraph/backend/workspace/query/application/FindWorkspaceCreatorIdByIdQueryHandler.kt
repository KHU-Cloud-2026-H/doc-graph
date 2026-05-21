package com.docgraph.backend.workspace.query.application

import com.docgraph.backend.workspace.command.domain.WorkspaceRepository
import org.springframework.stereotype.Service

@Service
class FindWorkspaceCreatorIdByIdQueryHandler(
    private val workspaceRepository: WorkspaceRepository,
) : FindWorkspaceCreatorIdByIdQuery {
    override fun find(workspaceId: Long): Long? =
        workspaceRepository.findById(workspaceId)?.createdBy
}
