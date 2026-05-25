package com.docgraph.backend.workspace.query.application

import com.docgraph.backend.document.query.application.SearchDocumentStatsByProjectIdsQuery
import com.docgraph.backend.project.query.application.SearchProjectIdsByWorkspaceIdsQuery
import com.docgraph.backend.validation.query.application.CountUnresolvedConflictsByProjectIdsQuery
import com.docgraph.backend.workspace.query.infra.WorkspaceQueryRepository
import org.springframework.stereotype.Service

@Service
class SearchAccessibleWorkspacesQueryHandler(
    private val queryRepository: WorkspaceQueryRepository,
    private val searchProjectIdsByWorkspaces: SearchProjectIdsByWorkspaceIdsQuery,
    private val searchDocumentStats: SearchDocumentStatsByProjectIdsQuery,
    private val countUnresolvedConflicts: CountUnresolvedConflictsByProjectIdsQuery,
) : SearchAccessibleWorkspacesQuery {
    override fun search(userId: Long): List<WorkspaceSummary> {
        val refs = queryRepository.findAccessibleWorkspaceRefs(userId)
        if (refs.isEmpty()) return emptyList()
        val workspaceIds = refs.map { it.id }
        val memberCounts = queryRepository.findMemberCountsByWorkspaceIds(workspaceIds)
        val projectsByWorkspace = searchProjectIdsByWorkspaces.search(workspaceIds)
        val allProjectIds = projectsByWorkspace.values.flatten().toSet()
        val documentStats = searchDocumentStats.search(allProjectIds)
        val conflictCounts = countUnresolvedConflicts.count(allProjectIds)
        return refs.map { ref ->
            val projectIds = projectsByWorkspace[ref.id].orEmpty()
            WorkspaceSummary(
                id = ref.id,
                name = ref.name,
                memberCount = memberCounts[ref.id] ?: 0,
                projectCount = projectIds.size,
                documentCount = projectIds.sumOf { documentStats[it]?.documentCount ?: 0L },
                unresolvedConflictCount = projectIds.sumOf { conflictCounts[it] ?: 0L },
                lastNotionChangedAt = projectIds.mapNotNull { documentStats[it]?.lastNotionChangedAt }.maxOrNull(),
            )
        }
    }
}
