package com.docgraph.backend.document.command.application

import com.docgraph.backend.auth.command.domain.NotionConnectionRepository
import com.docgraph.backend.auth.command.infra.NotionAccessTokenDecryptor
import com.docgraph.backend.document.command.domain.Block
import com.docgraph.backend.document.command.domain.BlockRepository
import com.docgraph.backend.document.command.domain.Document
import com.docgraph.backend.document.command.domain.DocumentRepository
import com.docgraph.backend.document.command.domain.NotionBlock
import com.docgraph.backend.document.command.domain.NotionDocumentClient
import com.docgraph.backend.document.command.domain.NotionIcon
import com.docgraph.backend.document.command.domain.NotionIconType
import com.docgraph.backend.document.command.domain.NotionPage
import com.docgraph.backend.document.query.application.DocumentType
import com.docgraph.backend.document.query.application.IconType
import com.docgraph.backend.graph.command.application.RegisterDependencyEdgeCommand
import com.docgraph.backend.graph.command.application.RegisterDependencyEdgeCommandHandler
import com.docgraph.backend.graph.command.domain.DependencyEdgeSource
import com.docgraph.backend.graph.command.domain.GraphRuleRepository
import com.docgraph.backend.project.command.domain.ProjectRepository
import com.docgraph.backend.project.query.application.FindProjectDetailByIdQuery
import com.docgraph.backend.project.query.application.SearchCategoriesByProjectQuery
import com.docgraph.backend.workspace.command.domain.WorkspaceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SyncProjectDocumentsCommandHandler(
    private val findProjectDetailById: FindProjectDetailByIdQuery,
    private val searchCategoriesByProject: SearchCategoriesByProjectQuery,
    private val notionDocumentClient: NotionDocumentClient,
    private val documentRepository: DocumentRepository,
    private val blockRepository: BlockRepository,
    private val graphRuleRepository: GraphRuleRepository,
    private val registerDependencyEdgeHandler: RegisterDependencyEdgeCommandHandler,
    private val projectRepository: ProjectRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val notionConnectionRepository: NotionConnectionRepository,
    private val notionAccessTokenDecryptor: NotionAccessTokenDecryptor,
) {
    @Transactional
    fun handle(command: SyncProjectDocumentsCommand) {
        val project = findProjectDetailById.find(command.projectId, command.requestedBy)
            ?: return
        val categoryTypes = searchCategoriesByProject.search(command.projectId)
            .associate { notionPageKey(it.notionPageId) to it.documentType }
        val synced = linkedMapOf<String, SyncedDocument>()
        val accessToken = findWorkspaceAccessToken(command.projectId, command.requestedBy)

        syncReachablePages(
            projectId = command.projectId,
            rootPageId = project.notionRootPageId,
            categoryTypes = categoryTypes,
            synced = synced,
            accessToken = accessToken,
        )

        createEdges(command.projectId, synced.values.toList())
    }

    private fun syncReachablePages(
        projectId: Long,
        rootPageId: String,
        categoryTypes: Map<String, DocumentType>,
        synced: MutableMap<String, SyncedDocument>,
        accessToken: String?,
    ) {
        val queue = ArrayDeque<PageSyncJob>()
        val queued = mutableSetOf<String>()
        fun enqueue(job: PageSyncJob) {
            if (queued.add(notionPageKey(job.pageId))) {
                queue += job
            }
        }

        enqueue(PageSyncJob(pageId = rootPageId))
        while (queue.isNotEmpty()) {
            val job = queue.removeFirst()
            val pageKey = notionPageKey(job.pageId)
            if (synced.containsKey(pageKey)) {
                continue
            }

            val page = notionDocumentClient.fetchPage(job.pageId, accessToken)
            val blocks = fetchBlockTree(page.id, accessToken)
            val type = categoryTypes[notionPageKey(page.id)] ?: job.inheritedType
            val flatText = blocks.mapNotNull { it.text ?: it.childPageTitle }
                .joinToString("\n")
                .ifBlank { null }

            val document = upsertDocument(
                projectId = projectId,
                page = page,
                parentNotionPageId = job.parentNotionPageId,
                parentDocumentId = job.parentDocumentId,
                type = type,
                flatText = flatText,
            )
            replaceBlocks(document, blocks)

            val linkedPageIds = blocks.flatMap { it.linkedPageIds }.toSet()
            synced[notionPageKey(page.id)] = SyncedDocument(
                document = document,
                linkedPageIds = linkedPageIds,
            )

            blocks.filter { it.type == "child_page" }.forEach { child ->
                enqueue(
                    PageSyncJob(
                        pageId = child.id,
                        parentNotionPageId = page.id,
                        parentDocumentId = document.id,
                        inheritedType = type,
                    ),
                )
            }
            linkedPageIds.forEach { linkedPageId ->
                enqueue(
                    PageSyncJob(
                        pageId = linkedPageId,
                        inheritedType = type,
                    ),
                )
            }
        }
    }

    private fun fetchBlockTree(parentBlockId: String, accessToken: String?): List<NotionBlock> {
        val result = mutableListOf<NotionBlock>()

        fun visit(blockId: String) {
            notionDocumentClient.fetchBlockChildren(blockId, accessToken).forEach { block ->
                result += block
                if (block.hasChildren && block.type != "child_page") {
                    visit(block.id)
                }
            }
        }

        visit(parentBlockId)
        return result
    }

    private fun findWorkspaceAccessToken(projectId: Long, userId: Long): String? {
        val project = projectRepository.findById(projectId) ?: return null
        val workspace = workspaceRepository.findById(project.workspaceId) ?: return null
        val connection = notionConnectionRepository.findByUserIdAndNotionWorkspaceId(
            userId = userId,
            notionWorkspaceId = workspace.notionWorkspaceId,
        ) ?: return null
        if (connection.revokedAt != null) {
            return null
        }
        return notionAccessTokenDecryptor.decrypt(connection.accessTokenEncrypted)
    }

    private fun upsertDocument(
        projectId: Long,
        page: NotionPage,
        parentNotionPageId: String?,
        parentDocumentId: Long?,
        type: DocumentType?,
        flatText: String?,
    ): Document {
        val document = documentRepository.findByProjectIdAndNotionPageId(projectId, page.id)
            ?: Document(
                projectId = projectId,
                notionPageId = page.id,
                title = page.title,
            )
        val (iconType, iconValue) = page.icon.toIconFields()
        document.refreshSnapshot(
            title = page.title,
            parentNotionPageId = parentNotionPageId,
            type = type,
            iconType = iconType,
            iconValue = iconValue,
            assigneeMemberId = document.assigneeMemberId,
            rawContent = page.rawJson,
            flatText = flatText,
            notionCreatedBy = page.createdBy,
            notionLastEditedBy = page.lastEditedBy,
            notionLastEditedAt = page.lastEditedTime,
        )
        document.parentDocumentId = parentDocumentId
        return documentRepository.save(document)
    }

    private fun replaceBlocks(document: Document, blocks: List<NotionBlock>) {
        val existingBlocks = blockRepository.findByDocument_IdOrderBySortOrderAsc(document.id)
            .associateBy { it.notionBlockId }
        val syncedBlockIds = blocks.mapTo(mutableSetOf()) { it.id }
        val obsoleteBlocks = existingBlocks.values.filterNot { it.notionBlockId in syncedBlockIds }
        if (obsoleteBlocks.isNotEmpty()) {
            blockRepository.deleteAll(obsoleteBlocks)
        }

        blockRepository.saveAll(
            blocks.mapIndexed { index, block ->
                val text = block.text ?: block.childPageTitle
                existingBlocks[block.id]?.apply {
                    refreshSnapshot(
                        parentType = block.parentType,
                        parentId = block.parentId,
                        type = block.type,
                        text = text,
                        sortOrder = index,
                        createdTime = block.createdTime,
                        createdBy = block.createdBy,
                        lastEditedTime = block.lastEditedTime,
                        lastEditedBy = block.lastEditedBy,
                        hasChildren = block.hasChildren,
                        archived = block.archived,
                        inTrash = block.inTrash,
                        rawBlock = block.rawJson,
                    )
                } ?: Block(
                    document = document,
                    notionBlockId = block.id,
                    parentType = block.parentType,
                    parentId = block.parentId,
                    type = block.type,
                    text = text,
                    sortOrder = index,
                    createdTime = block.createdTime,
                    createdBy = block.createdBy,
                    lastEditedTime = block.lastEditedTime,
                    lastEditedBy = block.lastEditedBy,
                    hasChildren = block.hasChildren,
                    archived = block.archived,
                    inTrash = block.inTrash,
                    rawBlock = block.rawJson,
                )
            },
        )
    }

    private fun createEdges(projectId: Long, documents: List<SyncedDocument>) {
        val byNotionPageId = documents.associateBy { notionPageKey(it.document.notionPageId) }
        documents.forEach { source ->
            val sourceType = source.document.type ?: return@forEach
            source.linkedPageIds.forEach { linkedPageId ->
                val target = byNotionPageId[notionPageKey(linkedPageId)] ?: return@forEach
                if (source.document.id == target.document.id) {
                    return@forEach
                }
                val targetType = target.document.type ?: return@forEach
                graphRuleRepository.findAllByProjectIdAndTypePair(projectId, sourceType, targetType)
                    .firstOrNull()
                    ?.let { rule ->
                        registerDependencyEdgeHandler.handle(
                            RegisterDependencyEdgeCommand(
                                projectId = projectId,
                                sourceDocumentId = source.document.id,
                                targetDocumentId = target.document.id,
                                ruleId = rule.id.takeIf { it != 0L },
                                validationCriterion = rule.validationCriterion,
                                source = DependencyEdgeSource.NOTION_REFERENCE,
                            ),
                        )
                    }
            }
        }
    }
}

private data class SyncedDocument(
    val document: Document,
    val linkedPageIds: Set<String>,
)

private data class PageSyncJob(
    val pageId: String,
    val parentNotionPageId: String? = null,
    val parentDocumentId: Long? = null,
    val inheritedType: DocumentType? = null,
)

private fun notionPageKey(id: String): String = id.replace("-", "").lowercase()

private fun NotionIcon?.toIconFields(): Pair<IconType?, String?> = when (this?.type) {
    NotionIconType.EMOJI -> IconType.EMOJI to value
    NotionIconType.EXTERNAL -> IconType.EXTERNAL to value
    NotionIconType.FILE -> IconType.FILE to value
    null -> null to null
}
