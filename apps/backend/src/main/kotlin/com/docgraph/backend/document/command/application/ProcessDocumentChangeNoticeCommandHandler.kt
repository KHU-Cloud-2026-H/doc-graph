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

    // 예외를 삼키지 않는다: transient(5xx·429·timeout)은 리스너 @Retryable이 재시도하고,
    // 영구 실패(비-429 4xx·파싱·빈 응답)는 @Recover가 markFailed로 dead-letter 전이한다.
    fun fetchFromNotion(notice: DocumentChangeNotice): NotionFetchResult {
        val accessToken = resolveAccessToken(notice)
        val page = notionDocumentClient.fetchPage(notice.notionPageId, accessToken)
        val blocks = fetchBlockTree(notice.notionPageId, accessToken)
        return NotionFetchResult(
            title = page.title,
            rawJson = page.rawJson,
            createdBy = page.createdBy,
            lastEditedBy = page.lastEditedBy,
            lastEditedTime = page.lastEditedTime,
            icon = page.icon,
            blocks = blocks,
        )
    }

    @Transactional
    fun markFailed(noticeId: Long, reason: String?) {
        val notice = noticeRepository.findById(noticeId).orElse(null) ?: return
        if (notice.status != OutboxStatus.PENDING) return
        notice.markFailed(reason)
        noticeRepository.save(notice)
    }

    @Transactional
    fun applyAndMarkSuccess(notice: DocumentChangeNotice, result: NotionFetchResult) {
        val matches = documentRepository.findByNotionPageId(notice.notionPageId)
        if (matches.isEmpty()) {
            notice.markFailed("document not found for notionPageId=${notice.notionPageId}")
            noticeRepository.save(notice)
            return
        }
        if (matches.size > 1) {
            // (project_id, notion_page_id) 유니크 보장으로 프로젝트당 최대 1건.
            // 2건 이상이면 동일 Notion 페이지가 여러 프로젝트에 등록된 경우이므로 전부 갱신해야
            // 일부 프로젝트 사본만 stale 되는 일이 없다.
            log.info(
                "notionPageId={}가 {}개 프로젝트에 매칭됨 — 전부 갱신한다. projectIds={}",
                notice.notionPageId, matches.size, matches.map { it.projectId },
            )
        }

        val flatText = result.blocks
            .mapNotNull { it.text ?: it.childPageTitle }
            .joinToString("\n")
            .ifBlank { null }
        val (iconType, iconValue, iconColor) = result.icon.toIconFields()
        matches.forEach { document ->
            document.refreshSnapshot(
                title = result.title,
                parentNotionPageId = document.parentNotionPageId,
                type = document.type,
                iconType = iconType,
                iconValue = iconValue,
                iconColor = iconColor,
                assigneeMemberId = document.assigneeMemberId,
                rawContent = result.rawJson,
                flatText = flatText,
                notionCreatedBy = result.createdBy,
                notionLastEditedBy = result.lastEditedBy,
                notionLastEditedAt = result.lastEditedTime,
            )
            documentRepository.save(document)
            replaceBlocks(document, result.blocks)

            publisher.publishEvent(
                DocumentContentChangedEvent(
                    documentId = document.id,
                    projectId = document.projectId,
                    notionPageId = document.notionPageId,
                ),
            )
        }

        notice.markSuccess()
        noticeRepository.save(notice)
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

private fun NotionIcon?.toIconFields(): Triple<IconType?, String?, String?> = when (this?.type) {
    NotionIconType.EMOJI -> Triple(IconType.EMOJI, value, null)
    NotionIconType.EXTERNAL -> Triple(IconType.EXTERNAL, value, null)
    NotionIconType.FILE -> Triple(IconType.FILE, value, null)
    NotionIconType.NATIVE -> Triple(IconType.NATIVE, value, color)
    NotionIconType.CUSTOM_EMOJI -> Triple(IconType.CUSTOM_EMOJI, value, null)
    null -> Triple(null, null, null)
}
