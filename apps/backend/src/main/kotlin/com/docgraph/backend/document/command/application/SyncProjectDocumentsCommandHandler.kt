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
import com.docgraph.backend.graph.command.application.ProposeEdgeCommand
import com.docgraph.backend.graph.command.application.ProposeEdgeCommandHandler
import com.docgraph.backend.graph.command.application.RegisterDependencyEdgeCommand
import com.docgraph.backend.graph.command.application.RegisterDependencyEdgeCommandHandler
import com.docgraph.backend.graph.command.domain.DependencyEdgeSource
import com.docgraph.backend.graph.command.domain.GraphRule
import com.docgraph.backend.graph.command.domain.GraphRuleRepository
import com.docgraph.backend.graph.command.domain.KeywordSimilarity
import com.docgraph.backend.project.command.domain.ProjectRepository
import com.docgraph.backend.project.query.application.FindProjectDetailByIdQuery
import com.docgraph.backend.project.query.application.SearchCategoriesByProjectQuery
import com.docgraph.backend.workspace.command.domain.WorkspaceRepository
import org.slf4j.LoggerFactory
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
    private val keywordSimilarity: KeywordSimilarity,
    private val registerDependencyEdgeHandler: RegisterDependencyEdgeCommandHandler,
    private val proposeEdgeHandler: ProposeEdgeCommandHandler,
    private val projectRepository: ProjectRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val notionConnectionRepository: NotionConnectionRepository,
    private val notionAccessTokenDecryptor: NotionAccessTokenDecryptor,
) {
    @Transactional
    fun handle(command: SyncProjectDocumentsCommand) {
        logger.info(
            "프로젝트 동기화 시작 — projectId={} requestedBy={}",
            command.projectId, command.requestedBy,
        )
        val project = findProjectDetailById.find(command.projectId, command.requestedBy)
            ?: run {
                logger.warn(
                    "프로젝트 동기화 중단 — 프로젝트 조회 실패 또는 접근 권한 없음: projectId={} requestedBy={}",
                    command.projectId, command.requestedBy,
                )
                return
            }
        val categoryTypes = searchCategoriesByProject.search(command.projectId)
            .associate { notionPageKey(it.notionPageId) to it.documentType }
        val synced = linkedMapOf<String, SyncedDocument>()
        val accessToken = findWorkspaceAccessToken(command.projectId, command.requestedBy)
        if (accessToken == null) {
            logger.warn(
                "Notion access token 미확보 — 모든 페이지 조회가 실패해 동기화 결과가 비어있을 수 있음: projectId={} requestedBy={}",
                command.projectId, command.requestedBy,
            )
        }

        val skippedPages = syncReachablePages(
            projectId = command.projectId,
            rootPageId = project.notionRootPageId,
            categoryTypes = categoryTypes,
            synced = synced,
            accessToken = accessToken,
        )

        val syncedDocuments = synced.values.toList()
        val edgeCount = createEdges(command.projectId, syncedDocuments)
        val proposalCount = createProposals(command.projectId, syncedDocuments)
        logger.info(
            "프로젝트 동기화 완료 — projectId={} 문서={}건 페이지skip={}건 엣지={}건 제안={}건",
            command.projectId, syncedDocuments.size, skippedPages, edgeCount, proposalCount,
        )
    }

    private fun syncReachablePages(
        projectId: Long,
        rootPageId: String,
        categoryTypes: Map<String, DocumentType>,
        synced: MutableMap<String, SyncedDocument>,
        accessToken: String?,
    ): Int {
        val rootKey = notionPageKey(rootPageId)
        var skippedPages = 0
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

            val page = try {
                notionDocumentClient.fetchPage(job.pageId, accessToken)
            } catch (e: Exception) {
                // 접근 불가 페이지(404, 권한 없음 등) skip — 나머지 페이지는 계속 처리.
                // 루트 실패는 동기화 결과가 통째로 비므로 warn, 말단(링크/하위) 실패는 정상 범위라 debug.
                skippedPages++
                if (pageKey == rootKey) {
                    logger.warn(
                        "루트 페이지 조회 실패 — 동기화 결과가 비어있을 수 있음: projectId={} rootPageId={}: {}",
                        projectId, job.pageId, e.message,
                    )
                } else {
                    logger.debug(
                        "페이지 조회 실패 skip — projectId={} pageId={}: {}",
                        projectId, job.pageId, e.message,
                    )
                }
                continue
            }
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
        return skippedPages
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
        val (iconType, iconValue, iconColor) = page.icon.toIconFields()
        document.refreshSnapshot(
            title = page.title,
            parentNotionPageId = parentNotionPageId,
            type = type,
            iconType = iconType,
            iconValue = iconValue,
            iconColor = iconColor,
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

    private fun createEdges(projectId: Long, documents: List<SyncedDocument>): Int {
        val byNotionPageId = documents.associateBy { notionPageKey(it.document.notionPageId) }
        var registered = 0
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
                        registered++
                    }
            }
        }
        return registered
    }

    /**
     * Notion 링크·멘션으로 엣지가 만들어지지 않은 문서 쌍 중 룰 타입 조합에 맞는 후보를
     * 키워드 유사도로 점수화해 source 문서당 상위 N개를 EdgeProposal로 제안한다.
     */
    private fun createProposals(projectId: Long, documents: List<SyncedDocument>): Int {
        val byNotionPageId = documents.associateBy { notionPageKey(it.document.notionPageId) }
        val diagnostics = ProposalDiagnostics()
        var proposed = 0
        documents.forEach { source ->
            val sourceType = source.document.type ?: run {
                diagnostics.sourceNoType++
                return@forEach
            }
            diagnostics.sourcesEvaluated++
            val linkedDocumentIds = source.linkedPageIds
                .mapNotNull { byNotionPageId[notionPageKey(it)]?.document?.id }
                .toSet()

            // 임계 필터를 적용하기 전 모든 후보의 점수를 산출해 둔다.
            // 임계 미만 후보도 최고 점수를 남겨야 "임계가 빡센지(점수 분포 문제)" vs
            // "룰·타입이 없는지(데이터 문제)"를 진단 로그로 가를 수 있다.
            val scored = documents.asSequence()
                .filter { it.document.id != source.document.id }
                .filter { it.document.id !in linkedDocumentIds }
                .mapNotNull { target ->
                    val targetType = target.document.type ?: run {
                        diagnostics.candidateNoType++
                        return@mapNotNull null
                    }
                    val rule = graphRuleRepository
                        .findAllByProjectIdAndTypePair(projectId, sourceType, targetType)
                        .firstOrNull() ?: run {
                            diagnostics.candidateNoRule++
                            return@mapNotNull null
                        }
                    val score = keywordSimilarity.score(source.document.flatText, target.document.flatText)
                    diagnostics.candidateScored++
                    ScoredCandidate(target = target, rule = rule, score = score)
                }
                .sortedByDescending { it.score }
                .toList()

            val bestScore = scored.firstOrNull()?.score
            if (bestScore != null) {
                diagnostics.recordBestScore(bestScore)
            }

            val eligible = scored.filter { it.score >= KeywordSimilarity.PROPOSAL_SCORE_THRESHOLD }
            if (scored.isNotEmpty() && eligible.isEmpty()) {
                diagnostics.sourcesAllBelowThreshold++
            }

            if (logger.isDebugEnabled) {
                logger.debug(
                    "제안 후보 평가 — projectId={} sourceDocumentId={} flatText={} 채점후보={}건 " +
                        "최고점수={} 임계이상={}건 임계값={} 상위후보={}",
                    projectId,
                    source.document.id,
                    if (source.document.flatText.isNullOrBlank()) "비어있음" else "있음",
                    scored.size,
                    bestScore?.let { "%.3f".format(it) } ?: "없음",
                    eligible.size,
                    KeywordSimilarity.PROPOSAL_SCORE_THRESHOLD,
                    scored.take(KeywordSimilarity.MAX_PROPOSALS_PER_SOURCE)
                        .joinToString { "doc=${it.target.document.id}:%.3f".format(it.score) },
                )
            }

            eligible.take(KeywordSimilarity.MAX_PROPOSALS_PER_SOURCE)
                .forEach { candidate ->
                    proposeEdgeHandler.handle(
                        ProposeEdgeCommand(
                            projectId = projectId,
                            sourceDocumentId = source.document.id,
                            targetDocumentId = candidate.target.document.id,
                            ruleId = candidate.rule.id.takeIf { it != 0L },
                            validationCriterion = candidate.rule.validationCriterion,
                            similarityScore = candidate.score,
                        ),
                    )
                    proposed++
                }
        }
        logger.info(
            "제안 생성 진단 — projectId={} source문서={}건(타입없음skip={}) " +
                "후보탈락[타입없음={} 룰없음={}] 채점={}건 " +
                "임계미만으로source전체탈락={}건 최고점수분포{} 임계값={} 제안확정={}건",
            projectId,
            diagnostics.sourcesEvaluated,
            diagnostics.sourceNoType,
            diagnostics.candidateNoType,
            diagnostics.candidateNoRule,
            diagnostics.candidateScored,
            diagnostics.sourcesAllBelowThreshold,
            diagnostics.bestScoreHistogram(),
            KeywordSimilarity.PROPOSAL_SCORE_THRESHOLD,
            proposed,
        )
        return proposed
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SyncProjectDocumentsCommandHandler::class.java)
    }
}

private data class ScoredCandidate(
    val target: SyncedDocument,
    val rule: GraphRule,
    val score: Double,
)

/**
 * 연결 제안 생성 한 회차의 탈락 사유·점수 분포를 누적한다.
 *
 * 연결이 거의 안 될 때 원인이 데이터(룰·타입 미설정)인지 점수(임계가 빡셈)인지 가르기 위한 진단용.
 * 최고점수 히스토그램에서 source들이 임계(0.1) 바로 아래에 몰려 있으면 임계 하향·fallback이,
 * 룰없음 탈락이 대부분이면 GraphRule 설정이 우선 처방이다.
 */
private class ProposalDiagnostics {
    var sourcesEvaluated = 0
    var sourceNoType = 0
    var candidateNoType = 0
    var candidateNoRule = 0
    var candidateScored = 0
    var sourcesAllBelowThreshold = 0

    // 최고점수 구간별 source 수: [0, (0,0.05), [0.05,0.1), [0.1,0.3), [0.3,1.0]]
    private val zero = "0"
    private val below005 = "~0.05"
    private val below010 = "0.05~0.1"
    private val below030 = "0.1~0.3"
    private val atLeast030 = "0.3~"
    private val histogram = linkedMapOf(
        zero to 0, below005 to 0, below010 to 0, below030 to 0, atLeast030 to 0,
    )

    fun recordBestScore(score: Double) {
        val bucket = when {
            score <= 0.0 -> zero
            score < 0.05 -> below005
            score < 0.1 -> below010
            score < 0.3 -> below030
            else -> atLeast030
        }
        histogram[bucket] = (histogram[bucket] ?: 0) + 1
    }

    fun bestScoreHistogram(): String =
        histogram.entries.joinToString(prefix = "{", postfix = "}") { "${it.key}=${it.value}" }
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

private fun NotionIcon?.toIconFields(): Triple<IconType?, String?, String?> = when (this?.type) {
    NotionIconType.EMOJI -> Triple(IconType.EMOJI, value, null)
    NotionIconType.EXTERNAL -> Triple(IconType.EXTERNAL, value, null)
    NotionIconType.FILE -> Triple(IconType.FILE, value, null)
    NotionIconType.NATIVE -> Triple(IconType.NATIVE, value, color)
    NotionIconType.CUSTOM_EMOJI -> Triple(IconType.CUSTOM_EMOJI, value, null)
    null -> Triple(null, null, null)
}
