package com.docgraph.backend.project.query.application

import com.docgraph.backend.auth.query.application.SearchUserAccountsByIdsQuery
import com.docgraph.backend.document.query.application.SearchDocumentStatsByProjectIdsQuery
import com.docgraph.backend.project.query.infra.ProjectQueryRepository
import com.docgraph.backend.validation.query.application.CountUnresolvedConflictsByProjectIdsQuery
import org.springframework.stereotype.Service

@Service
class FindProjectDetailByIdQueryHandler(
    private val repo: ProjectQueryRepository,
    private val searchUserAccountsByIds: SearchUserAccountsByIdsQuery,
    private val searchDocumentStats: SearchDocumentStatsByProjectIdsQuery,
    private val countUnresolvedConflicts: CountUnresolvedConflictsByProjectIdsQuery,
) : FindProjectDetailByIdQuery {
    override fun find(projectId: Long, userId: Long): ProjectDetail? {
        val project = repo.findProjectIfAccessible(projectId, userId) ?: return null
        val members = repo.findMembersByProjectId(projectId)
        val accounts = searchUserAccountsByIds.search(members.map { it.userId })
            .associateBy { it.id }
        val stats = searchDocumentStats.search(listOf(projectId))[projectId]
        val conflictCount = countUnresolvedConflicts.count(listOf(projectId))[projectId] ?: 0L
        return ProjectDetail(
            id = project.id,
            name = project.name,
            notionRootPageId = project.notionRootPageId,
            members = members.map {
                ProjectMemberSummary(
                    id = it.id,
                    name = accounts[it.userId]?.name ?: "",
                    role = it.role,
                )
            },
            memberCount = members.size,
            documentCount = stats?.documentCount ?: 0L,
            unresolvedConflictCount = conflictCount,
            lastNotionChangedAt = stats?.lastNotionChangedAt,
        )
    }
}
