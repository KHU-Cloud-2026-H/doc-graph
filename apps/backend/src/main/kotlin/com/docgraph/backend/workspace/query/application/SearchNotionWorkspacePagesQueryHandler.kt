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
) : SearchNotionWorkspacePagesQuery {

    override fun search(workspaceId: Long, userId: Long, query: String?): List<NotionWorkspacePageResponse>? {
        workspaceQueryRepository.findWorkspaceSummaryAccessibleBy(workspaceId, userId) ?: return null
        val workspace = workspaceRepository.findById(workspaceId) ?: return null
        val connection = notionConnectionRepository.findByUserIdAndNotionWorkspaceId(
            userId = userId,
            notionWorkspaceId = workspace.notionWorkspaceId,
        ) ?: return null
        if (connection.revokedAt != null) {
            return emptyList()
        }

        val accessToken = notionAccessTokenDecryptor.decrypt(connection.accessTokenEncrypted)
        return notionDocumentClient.searchPages(accessToken, query)
            .map {
                NotionWorkspacePageResponse(
                    id = it.id,
                    title = it.title,
                    url = it.url,
                    lastEditedTime = it.lastEditedTime,
                )
            }
    }
}
