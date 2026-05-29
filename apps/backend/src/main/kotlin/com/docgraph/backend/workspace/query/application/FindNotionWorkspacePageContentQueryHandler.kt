package com.docgraph.backend.workspace.query.application

import com.docgraph.backend.auth.command.domain.NotionConnectionRepository
import com.docgraph.backend.auth.command.infra.NotionAccessTokenDecryptor
import com.docgraph.backend.document.command.domain.NotionBlock
import com.docgraph.backend.document.command.domain.NotionDocumentClient
import com.docgraph.backend.workspace.command.domain.WorkspaceRepository
import com.docgraph.backend.workspace.query.infra.WorkspaceQueryRepository
import org.springframework.stereotype.Service

@Service
class FindNotionWorkspacePageContentQueryHandler(
    private val workspaceQueryRepository: WorkspaceQueryRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val notionConnectionRepository: NotionConnectionRepository,
    private val notionAccessTokenDecryptor: NotionAccessTokenDecryptor,
    private val notionDocumentClient: NotionDocumentClient,
) : FindNotionWorkspacePageContentQuery {

    override fun find(workspaceId: Long, userId: Long, pageId: String): NotionWorkspacePageContentResponse? {
        workspaceQueryRepository.findWorkspaceRefAccessibleBy(workspaceId, userId) ?: return null
        val workspace = workspaceRepository.findById(workspaceId) ?: return null
        val connection = notionConnectionRepository.findByUserIdAndNotionWorkspaceId(
            userId = userId,
            notionWorkspaceId = workspace.notionWorkspaceId,
        ) ?: return null
        if (connection.revokedAt != null) {
            return null
        }

        val accessToken = notionAccessTokenDecryptor.decrypt(connection.accessTokenEncrypted)
        val page = notionDocumentClient.fetchPage(pageId, accessToken)
        val blocks = fetchBlockTree(page.id, accessToken)
        val flatText = blocks.mapNotNull { it.text ?: it.childPageTitle }
            .joinToString("\n")
            .ifBlank { null }

        return NotionWorkspacePageContentResponse(
            id = page.id,
            title = page.title,
            flatText = flatText,
            lastEditedTime = page.lastEditedTime,
            blocks = blocks.mapIndexed { index, block ->
                NotionWorkspaceBlockResponse(
                    id = block.id,
                    type = block.type,
                    parentId = block.parentId,
                    text = block.text,
                    childPageTitle = block.childPageTitle,
                    linkedPageIds = block.linkedPageIds,
                    hasChildren = block.hasChildren,
                    archived = block.archived,
                    inTrash = block.inTrash,
                    sortOrder = index,
                )
            },
        )
    }

    private fun fetchBlockTree(pageId: String, accessToken: String): List<NotionBlock> {
        val result = mutableListOf<NotionBlock>()

        fun visit(blockId: String) {
            notionDocumentClient.fetchBlockChildren(blockId, accessToken).forEach { block ->
                result += block
                if (block.hasChildren && block.type != "child_page") {
                    visit(block.id)
                }
            }
        }

        visit(pageId)
        return result
    }
}
