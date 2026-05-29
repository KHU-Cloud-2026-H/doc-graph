package com.docgraph.backend.workspace.query.application

import com.docgraph.backend.auth.command.domain.NotionConnectionRepository
import com.docgraph.backend.auth.command.infra.NotionAccessTokenDecryptor
import com.docgraph.backend.document.command.domain.NotionDocumentClient
import com.docgraph.backend.workspace.command.domain.WorkspaceRepository
import com.docgraph.backend.workspace.query.infra.WorkspaceQueryRepository
import org.springframework.stereotype.Service

@Service
class SearchNotionWorkspacePagesQueryHandler(
    private val workspaceQueryRepository: WorkspaceQueryRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val notionConnectionRepository: NotionConnectionRepository,
    private val notionAccessTokenDecryptor: NotionAccessTokenDecryptor,
    private val notionDocumentClient: NotionDocumentClient,
) : SearchNotionWorkspacePagesQuery,
    SearchNotionRootPagesQuery,
    SearchNotionPageChildrenQuery,
    FindNotionPageMetadataQuery {

    override fun search(workspaceId: Long, userId: Long, query: String?): List<NotionWorkspacePageResponse>? {
        val accessToken = findAccessToken(workspaceId, userId) ?: return null
        return notionDocumentClient.searchPages(accessToken, query)
            .map { page ->
                NotionWorkspacePageResponse(
                    notionPageId = page.id,
                    title = page.title,
                    icon = page.icon.toIconResponse(),
                    url = page.url,
                    lastEditedTime = page.lastEditedTime,
                )
            }
    }

    override fun searchRootPages(workspaceId: Long, userId: Long): List<NotionWorkspacePageResponse>? {
        val accessToken = findAccessToken(workspaceId, userId) ?: return null
        return notionDocumentClient.searchPages(accessToken, query = null)
            .filter { it.parentType == "workspace" }
            .map { page ->
                NotionWorkspacePageResponse(
                    notionPageId = page.id,
                    title = page.title,
                    icon = page.icon.toIconResponse(),
                    url = page.url,
                    lastEditedTime = page.lastEditedTime,
                )
            }
    }

    override fun searchPageChildren(workspaceId: Long, userId: Long, pageId: String): List<NotionWorkspacePageResponse>? {
        val accessToken = findAccessToken(workspaceId, userId) ?: return null
        return notionDocumentClient.fetchBlockChildren(pageId, accessToken)
            .filter { it.type == "child_page" && !it.archived && !it.inTrash }
            .map { block ->
                val page = notionDocumentClient.fetchPage(block.id, accessToken)
                NotionWorkspacePageResponse(
                    notionPageId = page.id,
                    title = page.title.ifBlank { block.childPageTitle ?: "Untitled" },
                    icon = page.icon.toIconResponse(),
                    url = null,
                    lastEditedTime = page.lastEditedTime,
                )
            }
    }

    override fun find(workspaceId: Long, userId: Long, pageId: String): NotionWorkspacePageMetadataResponse? {
        val accessToken = findAccessToken(workspaceId, userId) ?: return null
        val page = notionDocumentClient.fetchPage(pageId, accessToken)
        return NotionWorkspacePageMetadataResponse(
            title = page.title,
            icon = page.icon.toIconResponse(),
        )
    }

    private fun findAccessToken(workspaceId: Long, userId: Long): String? {
        workspaceQueryRepository.findWorkspaceSummaryAccessibleBy(workspaceId, userId) ?: return null
        val workspace = workspaceRepository.findById(workspaceId) ?: return null
        val connection = notionConnectionRepository.findByUserIdAndNotionWorkspaceId(
            userId = userId,
            notionWorkspaceId = workspace.notionWorkspaceId,
        ) ?: return null
        if (connection.revokedAt != null) {
            return null
        }

        return notionAccessTokenDecryptor.decrypt(connection.accessTokenEncrypted)
    }
}
