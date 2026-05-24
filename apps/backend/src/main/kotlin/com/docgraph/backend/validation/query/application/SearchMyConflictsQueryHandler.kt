package com.docgraph.backend.validation.query.application

import com.docgraph.backend.auth.query.application.GetCurrentUserIdQuery
import com.docgraph.backend.document.query.application.SearchDocumentIdsByAssigneeQuery
import com.docgraph.backend.document.query.application.SearchDocumentIdsByProjectAndTypesQuery
import com.docgraph.backend.document.query.application.SearchDocumentReferencesByIdsQuery
import com.docgraph.backend.document.query.application.SearchUnassignedDocumentIdsByProjectQuery
import com.docgraph.backend.graph.query.application.SearchEdgeDetailsByIdsQuery
import com.docgraph.backend.graph.query.application.SearchEdgeIdsByTargetDocumentIdsQuery
import com.docgraph.backend.project.query.application.SearchAdminProjectIdsByUserIdQuery
import com.docgraph.backend.project.query.application.SearchAssignedDocumentTypesByUserIdQuery
import com.docgraph.backend.project.query.application.SearchProjectSummariesByIdsQuery
import com.docgraph.backend.validation.query.infra.ValidationQueryRepository
import com.docgraph.backend.web.PageResponse
import com.docgraph.backend.workspace.query.application.SearchWorkspaceMemberIdsByUserIdQuery
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class SearchMyConflictsQueryHandler(
    private val getCurrentUserId: GetCurrentUserIdQuery,
    private val searchWorkspaceMemberIds: SearchWorkspaceMemberIdsByUserIdQuery,
    private val searchDocumentIdsByAssignee: SearchDocumentIdsByAssigneeQuery,
    private val searchAssignedDocumentTypes: SearchAssignedDocumentTypesByUserIdQuery,
    private val searchDocumentIdsByProjectAndTypes: SearchDocumentIdsByProjectAndTypesQuery,
    private val searchAdminProjectIds: SearchAdminProjectIdsByUserIdQuery,
    private val searchUnassignedDocumentIds: SearchUnassignedDocumentIdsByProjectQuery,
    private val searchDocumentReferences: SearchDocumentReferencesByIdsQuery,
    private val searchEdgeIdsByTargetDocuments: SearchEdgeIdsByTargetDocumentIdsQuery,
    private val searchEdgeDetailsByIds: SearchEdgeDetailsByIdsQuery,
    private val searchProjectSummaries: SearchProjectSummariesByIdsQuery,
    private val validationQueryRepository: ValidationQueryRepository,
) : SearchMyConflictsQuery {

    override fun search(filter: MyConflictStatusFilter, pageable: Pageable): PageResponse<MyConflictRow> {
        val userId = getCurrentUserId.get()

        val memberIds = searchWorkspaceMemberIds.search(userId)
        val directDocIds = memberIds.flatMap { searchDocumentIdsByAssignee.search(it) }
        val routedDocIds = searchDocumentIdsByProjectAndTypes.search(searchAssignedDocumentTypes.search(userId))
        val assigneeDocIds = (directDocIds + routedDocIds).distinct()

        val adminProjectIds = searchAdminProjectIds.search(userId)
        val unassignedDocIds = adminProjectIds.flatMap { searchUnassignedDocumentIds.search(it) }

        val targetDocIds = (assigneeDocIds + unassignedDocIds).distinct()
        if (targetDocIds.isEmpty()) return emptyPage(pageable)

        val targetRefs = searchDocumentReferences.search(targetDocIds)
        val edgeIds = targetRefs.groupBy { it.projectId }
            .flatMap { (projectId, docs) ->
                searchEdgeIdsByTargetDocuments.search(projectId, docs.map { it.id })
            }
            .distinct()
        if (edgeIds.isEmpty()) return emptyPage(pageable)

        val conflictPage = validationQueryRepository.findInboxConflictsByEdgeIds(edgeIds, filter, pageable)
        if (conflictPage.content.isEmpty()) {
            return PageResponse(
                content = emptyList(),
                totalElements = conflictPage.totalElements,
                totalPages = conflictPage.totalPages,
                page = conflictPage.number,
                size = conflictPage.size,
            )
        }

        val edgesInPage = searchEdgeDetailsByIds.search(conflictPage.content.map { it.edgeId })
        val edgeById = edgesInPage.associateBy { it.id }
        val sourceRefs = searchDocumentReferences.search(edgesInPage.map { it.sourceDocumentId }.distinct())
            .associateBy { it.id }
        val targetRefById = targetRefs.associateBy { it.id }
        val projectNameById = searchProjectSummaries.search(targetRefs.map { it.projectId }.distinct())
            .associate { it.id to it.name }

        val rows = conflictPage.content.mapNotNull { conflict ->
            val edge = edgeById[conflict.edgeId] ?: return@mapNotNull null
            val target = targetRefById[edge.targetDocumentId] ?: return@mapNotNull null
            val source = sourceRefs[edge.sourceDocumentId] ?: return@mapNotNull null
            MyConflictRow(
                id = conflict.id,
                edgeId = conflict.edgeId,
                projectId = target.projectId,
                projectName = projectNameById[target.projectId].orEmpty(),
                sourceDocument = InboxDocumentRef(source.id, source.title, source.type),
                targetDocument = InboxDocumentRef(target.id, target.title, target.type),
                status = if (conflict.ignoredAt != null) MyConflictStatus.IGNORED else MyConflictStatus.ACTIVE,
                firstDetectedAt = conflict.firstDetectedAt,
                ignoredAt = conflict.ignoredAt,
            )
        }

        return PageResponse(
            content = rows,
            totalElements = conflictPage.totalElements,
            totalPages = conflictPage.totalPages,
            page = conflictPage.number,
            size = conflictPage.size,
        )
    }

    private fun emptyPage(pageable: Pageable): PageResponse<MyConflictRow> = PageResponse(
        content = emptyList(),
        totalElements = 0L,
        totalPages = 0,
        page = pageable.pageNumber,
        size = pageable.pageSize,
    )
}
