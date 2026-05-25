package com.docgraph.backend.workspace.query.application

import com.docgraph.backend.auth.query.application.SearchUserAccountsByIdsQuery
import com.docgraph.backend.document.query.application.SearchDocumentStatsByProjectIdsQuery
import com.docgraph.backend.project.query.application.SearchProjectIdsByWorkspaceIdsQuery
import com.docgraph.backend.validation.query.application.CountUnresolvedConflictsByProjectIdsQuery
import com.docgraph.backend.workspace.query.infra.WorkspaceQueryRepository
import org.springframework.stereotype.Service

@Service
class FindWorkspaceDetailByIdQueryHandler(
    private val queryRepository: WorkspaceQueryRepository,
    private val searchUserAccountsByIds: SearchUserAccountsByIdsQuery,
    private val searchProjectIdsByWorkspaces: SearchProjectIdsByWorkspaceIdsQuery,
    private val searchDocumentStats: SearchDocumentStatsByProjectIdsQuery,
    private val countUnresolvedConflicts: CountUnresolvedConflictsByProjectIdsQuery,
) : FindWorkspaceDetailByIdQuery {

    override fun find(workspaceId: Long, userId: Long): WorkspaceDetail? {
        val ref = queryRepository.findWorkspaceRefAccessibleBy(workspaceId, userId)
            ?: return null
        val members = queryRepository.findMembersByWorkspaceId(workspaceId)
        val accounts = searchUserAccountsByIds.search(members.map { it.userId })
            .associateBy { it.id }
        val projectIds = searchProjectIdsByWorkspaces.search(listOf(workspaceId))[workspaceId].orEmpty()
        val documentStats = searchDocumentStats.search(projectIds)
        val conflictCounts = countUnresolvedConflicts.count(projectIds)
        return WorkspaceDetail(
            id = ref.id,
            name = ref.name,
            members = members.map { row ->
                val account = accounts[row.userId]
                WorkspaceMemberSummary(
                    userId = row.userId,
                    name = account?.name ?: "",
                    email = account?.email ?: "",
                    joinedAt = row.joinedAt,
                )
            },
            memberCount = members.size,
            projectCount = projectIds.size,
            documentCount = projectIds.sumOf { documentStats[it]?.documentCount ?: 0L },
            unresolvedConflictCount = projectIds.sumOf { conflictCounts[it] ?: 0L },
            lastNotionChangedAt = projectIds.mapNotNull { documentStats[it]?.lastNotionChangedAt }.maxOrNull(),
        )
    }
}
