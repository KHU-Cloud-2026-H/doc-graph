package com.docgraph.backend.workspace.query.application

import com.docgraph.backend.auth.query.application.SearchUserAccountsByIdsQuery
import com.docgraph.backend.workspace.query.infra.WorkspaceQueryRepository
import org.springframework.stereotype.Service

@Service
class FindWorkspaceDetailByIdQueryHandler(
    private val queryRepository: WorkspaceQueryRepository,
    private val searchUserAccountsByIds: SearchUserAccountsByIdsQuery,
) : FindWorkspaceDetailByIdQuery {

    override fun find(workspaceId: Long, userId: Long): WorkspaceDetail? {
        val summary = queryRepository.findWorkspaceSummaryAccessibleBy(workspaceId, userId)
            ?: return null
        val members = queryRepository.findMembersByWorkspaceId(workspaceId)
        val accounts = searchUserAccountsByIds.search(members.map { it.userId })
            .associateBy { it.id }
        return WorkspaceDetail(
            id = summary.id,
            name = summary.name,
            members = members.map { row ->
                val account = accounts[row.userId]
                WorkspaceMemberSummary(
                    userId = row.userId,
                    name = account?.name ?: "",
                    email = account?.email ?: "",
                    joinedAt = row.joinedAt,
                )
            },
        )
    }
}