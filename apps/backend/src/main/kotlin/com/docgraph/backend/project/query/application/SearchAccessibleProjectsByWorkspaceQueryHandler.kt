package com.docgraph.backend.project.query.application

import com.docgraph.backend.document.query.application.SearchDocumentStatsByProjectIdsQuery
import com.docgraph.backend.document.query.application.SearchPageInfoByNotionPageIdsQuery
import com.docgraph.backend.project.query.infra.ProjectQueryRepository
import com.docgraph.backend.validation.query.application.CountUnresolvedConflictsByProjectIdsQuery
import org.springframework.stereotype.Service

@Service
class SearchAccessibleProjectsByWorkspaceQueryHandler(
    private val repo: ProjectQueryRepository,
    private val searchDocumentStats: SearchDocumentStatsByProjectIdsQuery,
    private val countUnresolvedConflicts: CountUnresolvedConflictsByProjectIdsQuery,
    private val searchPageInfo: SearchPageInfoByNotionPageIdsQuery,
) : SearchAccessibleProjectsByWorkspaceQuery {
    override fun search(workspaceId: Long, userId: Long): List<ProjectSummary> {
        val refs = repo.findAccessibleProjectRefsByWorkspace(workspaceId, userId)
        if (refs.isEmpty()) return emptyList()
        val projectIds = refs.map { it.id }
        val memberCounts = repo.findProjectMemberCountsByIds(projectIds)
        val documentStats = searchDocumentStats.search(projectIds)
        val conflictCounts = countUnresolvedConflicts.count(projectIds)
        val rootPageIdByProject = repo.findNotionRootPageIdsByProjectIds(projectIds)
        val rootInfoByPageId = searchPageInfo.search(rootPageIdByProject.values.distinct())
            .associateBy { it.notionPageId }
        return refs.map { ref ->
            val stats = documentStats[ref.id]
            val rootInfo = rootPageIdByProject[ref.id]?.let { rootInfoByPageId[it] }
            ProjectSummary(
                id = ref.id,
                name = ref.name,
                memberCount = memberCounts[ref.id] ?: 0,
                documentCount = stats?.documentCount ?: 0L,
                unresolvedConflictCount = conflictCounts[ref.id] ?: 0L,
                lastNotionChangedAt = stats?.lastNotionChangedAt,
                rootPageTitle = rootInfo?.title,
                rootPageIcon = rootInfo?.icon,
            )
        }
    }
}
