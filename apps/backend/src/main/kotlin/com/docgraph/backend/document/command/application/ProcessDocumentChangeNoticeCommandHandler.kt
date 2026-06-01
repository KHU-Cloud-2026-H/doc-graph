package com.docgraph.backend.document.command.application

import com.docgraph.backend.auth.command.domain.NotionConnectionRepository
import com.docgraph.backend.auth.command.infra.NotionAccessTokenDecryptor
import com.docgraph.backend.document.command.domain.Block
import com.docgraph.backend.document.command.domain.BlockRepository
import com.docgraph.backend.document.command.domain.Document
import com.docgraph.backend.document.command.domain.DocumentChangeNotice
import com.docgraph.backend.document.command.domain.DocumentChangeNoticeRepository
import com.docgraph.backend.document.command.domain.DocumentContentChangedEvent
import com.docgraph.backend.document.command.domain.DocumentRepository
import com.docgraph.backend.document.command.domain.NotionBlock
import com.docgraph.backend.document.command.domain.NotionDocumentClient
import com.docgraph.backend.document.command.domain.NotionIcon
import com.docgraph.backend.document.command.domain.NotionIconType
import com.docgraph.backend.document.query.application.IconType
import com.docgraph.backend.event.OutboxStatus
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class ProcessDocumentChangeNoticeCommandHandler(
    private val noticeRepository: DocumentChangeNoticeRepository,
    private val documentRepository: DocumentRepository,
    private val blockRepository: BlockRepository,
    private val notionDocumentClient: NotionDocumentClient,
    private val notionConnectionRepository: NotionConnectionRepository,
    private val notionAccessTokenDecryptor: NotionAccessTokenDecryptor,
    private val publisher: ApplicationEventPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun recordAttempt(noticeId: Long): DocumentChangeNotice? {
        val notice = noticeRepository.findById(noticeId).orElse(null) ?: return null
        if (notice.status != OutboxStatus.PENDING) return null
        notice.recordAttempt()
        return noticeRepository.save(notice)
    }

    fun fetchFromNotion(notice: DocumentChangeNotice): NotionFetchResult? {
        val accessToken = resolveAccessToken(notice)
        return try {
            val page = notionDocumentClient.fetchPage(notice.notionPageId, accessToken)
            val blocks = fetchBlockTree(notice.notionPageId, accessToken)
            NotionFetchResult(
                title = page.title,
                rawJson = page.rawJson,
                createdBy = page.createdBy,
                lastEditedBy = page.lastEditedBy,
                lastEditedTime = page.lastEditedTime,
                icon = page.icon,
                blocks = blocks,
            )
        } catch (e: Exception) {
            log.warn("Notion API call failed for notice={} pageId={}", notice.id, notice.notionPageId, e)
            null
        }
    }

    @Transactional
    fun applyAndMarkSuccess(notice: DocumentChangeNotice, result: NotionFetchResult) {
        val document = documentRepository.findByNotionPageId(notice.notionPageId).firstOrNull()
        if (document == null) {
            notice.markFailed("document not found for notionPageId=${notice.notionPageId}")
            noticeRepository.save(notice)
            return
        }

        val flatText = result.blocks
            .mapNotNull { it.text ?: it.childPageTitle }
            .joinToString("\n")
            .ifBlank { null }
        val (iconType, iconValue) = result.icon.toIconFields()
        document.refreshSnapshot(
            title = result.title,
            parentNotionPageId = document.parentNotionPageId,
            type = document.type,
            iconType = iconType,
            iconValue = iconValue,
            assigneeMemberId = document.assigneeMemberId,
            rawContent = result.rawJson,
            flatText = flatText,
            notionCreatedBy = result.createdBy,
            notionLastEditedBy = result.lastEditedBy,
            notionLastEditedAt = result.lastEditedTime,
        )
        documentRepository.save(document)
        replaceBlocks(document, result.blocks)

        notice.markSuccess()
        noticeRepository.save(notice)

        publisher.publishEvent(
            DocumentContentChangedEvent(
                documentId = document.id,
                projectId = document.projectId,
                notionPageId = document.notionPageId,
            ),
        )
    }

    private fun fetchBlockTree(rootBlockId: String, accessToken: String?): List<NotionBlock> {
        val result = mutableListOf<NotionBlock>()
        fun visit(blockId: String) {
            notionDocumentClient.fetchBlockChildren(blockId, accessToken).forEach { block ->
                result += block
                if (block.hasChildren && block.type != "child_page") visit(block.id)
            }
        }
        visit(rootBlockId)
        return result
    }

    private fun replaceBlocks(document: Document, blocks: List<NotionBlock>) {
        val existing = blockRepository.findByDocument_IdOrderBySortOrderAsc(document.id)
            .associateBy { it.notionBlockId }
        val syncedIds = blocks.mapTo(mutableSetOf()) { it.id }
        val obsolete = existing.values.filterNot { it.notionBlockId in syncedIds }
        if (obsolete.isNotEmpty()) blockRepository.deleteAll(obsolete)

        blockRepository.saveAll(
            blocks.mapIndexed { index, block ->
                val text = block.text ?: block.childPageTitle
                existing[block.id]?.apply {
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

    private fun resolveAccessToken(notice: DocumentChangeNotice): String? {
        val workspaceId = notice.notionWorkspaceId ?: return null
        return notionConnectionRepository.findAllByNotionWorkspaceId(workspaceId)
            .firstOrNull { it.revokedAt == null }
            ?.let { notionAccessTokenDecryptor.decrypt(it.accessTokenEncrypted) }
    }
}

data class NotionFetchResult(
    val title: String,
    val rawJson: String,
    val createdBy: String?,
    val lastEditedBy: String?,
    val lastEditedTime: OffsetDateTime?,
    val icon: NotionIcon?,
    val blocks: List<NotionBlock>,
)

private fun NotionIcon?.toIconFields(): Pair<IconType?, String?> = when (this?.type) {
    NotionIconType.EMOJI -> IconType.EMOJI to value
    NotionIconType.EXTERNAL -> IconType.EXTERNAL to value
    NotionIconType.FILE -> IconType.FILE to value
    null -> null to null
}
