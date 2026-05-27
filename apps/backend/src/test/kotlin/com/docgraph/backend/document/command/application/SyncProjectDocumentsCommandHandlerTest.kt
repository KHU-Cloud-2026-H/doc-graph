package com.docgraph.backend.document.command.application

import com.docgraph.backend.auth.command.domain.NotionConnectionRepository
import com.docgraph.backend.auth.command.infra.NotionAccessTokenDecryptor
import com.docgraph.backend.document.command.domain.Block
import com.docgraph.backend.document.command.domain.BlockRepository
import com.docgraph.backend.document.command.domain.Document
import com.docgraph.backend.document.command.domain.DocumentRepository
import com.docgraph.backend.document.command.domain.NotionBlock
import com.docgraph.backend.document.command.domain.NotionDocumentClient
import com.docgraph.backend.document.command.domain.NotionPage
import com.docgraph.backend.document.query.application.DocumentType
import com.docgraph.backend.graph.command.application.RegisterDependencyEdgeCommand
import com.docgraph.backend.graph.command.application.RegisterDependencyEdgeCommandHandler
import com.docgraph.backend.graph.command.domain.DependencyEdgeSource
import com.docgraph.backend.graph.command.domain.GraphRule
import com.docgraph.backend.graph.command.domain.GraphRuleRepository
import com.docgraph.backend.project.command.domain.ProjectRepository
import com.docgraph.backend.project.query.application.CategoryProjection
import com.docgraph.backend.project.query.application.FindProjectDetailByIdQuery
import com.docgraph.backend.project.query.application.ProjectDetail
import com.docgraph.backend.project.query.application.SearchCategoriesByProjectQuery
import com.docgraph.backend.workspace.command.domain.WorkspaceRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@Tag("unit")
class SyncProjectDocumentsCommandHandlerTest {

    private val findProjectDetailById = mockk<FindProjectDetailByIdQuery>()
    private val searchCategoriesByProject = mockk<SearchCategoriesByProjectQuery>()
    private val notionDocumentClient = mockk<NotionDocumentClient>()
    private val documentRepository = mockk<DocumentRepository>()
    private val blockRepository = mockk<BlockRepository>()
    private val graphRuleRepository = mockk<GraphRuleRepository>()
    private val registerDependencyEdgeHandler = mockk<RegisterDependencyEdgeCommandHandler>(relaxed = true)
    private val projectRepository = mockk<ProjectRepository>()
    private val workspaceRepository = mockk<WorkspaceRepository>()
    private val notionConnectionRepository = mockk<NotionConnectionRepository>()
    private val notionAccessTokenDecryptor = mockk<NotionAccessTokenDecryptor>()

    private val handler = SyncProjectDocumentsCommandHandler(
        findProjectDetailById = findProjectDetailById,
        searchCategoriesByProject = searchCategoriesByProject,
        notionDocumentClient = notionDocumentClient,
        documentRepository = documentRepository,
        blockRepository = blockRepository,
        graphRuleRepository = graphRuleRepository,
        registerDependencyEdgeHandler = registerDependencyEdgeHandler,
        projectRepository = projectRepository,
        workspaceRepository = workspaceRepository,
        notionConnectionRepository = notionConnectionRepository,
        notionAccessTokenDecryptor = notionAccessTokenDecryptor,
    )

    @Test
    fun `초기 동기화는 Notion page와 block을 저장하고 mention 기반 edge 등록까지 연결한다`() {
        every { findProjectDetailById.find(1L, 9L) } returns ProjectDetail(
            id = 1L,
            name = "Project",
            notionRootPageId = "root-page",
            members = emptyList(),
            memberCount = 0,
            documentCount = 0L,
            unresolvedConflictCount = 0L,
            lastNotionChangedAt = null,
        )
        every { searchCategoriesByProject.search(1L) } returns listOf(
            CategoryProjection("source-page", DocumentType.REQUIREMENTS),
            CategoryProjection("target-page", DocumentType.DESIGN),
        )
        every { projectRepository.findById(1L) } returns null
        every { documentRepository.findByProjectIdAndNotionPageId(any(), any()) } returns null

        var nextDocumentId = 100L
        every { documentRepository.save(any()) } answers {
            firstArg<Document>().also { it.id = nextDocumentId++ }
        }
        every { blockRepository.findByDocument_IdOrderBySortOrderAsc(any()) } returns emptyList()
        every { blockRepository.deleteAll(any<Iterable<Block>>()) } returns Unit
        every { blockRepository.saveAll(any<Iterable<Block>>()) } answers {
            firstArg<Iterable<Block>>().toList()
        }

        every { notionDocumentClient.fetchPage("root-page", null) } returns page("root-page", "Root")
        every { notionDocumentClient.fetchBlockChildren("root-page", null) } returns listOf(
            block(id = "source-page", type = "child_page", childPageTitle = "Source"),
            block(id = "target-page", type = "child_page", childPageTitle = "Target"),
        )
        every { notionDocumentClient.fetchPage("source-page", null) } returns page("source-page", "Source")
        every { notionDocumentClient.fetchBlockChildren("source-page", null) } returns listOf(
            block(id = "paragraph-1", text = "See target", linkedPageIds = setOf("target-page")),
        )
        every { notionDocumentClient.fetchPage("target-page", null) } returns page("target-page", "Target")
        every { notionDocumentClient.fetchBlockChildren("target-page", null) } returns emptyList()

        every {
            graphRuleRepository.findAllByProjectIdAndTypePair(
                1L,
                DocumentType.REQUIREMENTS,
                DocumentType.DESIGN,
            )
        } returns listOf(
            GraphRule(
                id = 30L,
                projectId = 1L,
                sourceType = DocumentType.REQUIREMENTS,
                targetType = DocumentType.DESIGN,
                validationCriterion = "source must reflect target",
            ),
        )

        val commandSlot = slot<RegisterDependencyEdgeCommand>()

        handler.handle(SyncProjectDocumentsCommand(projectId = 1L, requestedBy = 9L))

        verify(exactly = 3) { documentRepository.save(any()) }
        verify(exactly = 1) {
            registerDependencyEdgeHandler.handle(capture(commandSlot))
        }
        assertEquals(1L, commandSlot.captured.projectId)
        assertEquals(101L, commandSlot.captured.sourceDocumentId)
        assertEquals(102L, commandSlot.captured.targetDocumentId)
        assertEquals(30L, commandSlot.captured.ruleId)
        assertEquals("source must reflect target", commandSlot.captured.validationCriterion)
        assertEquals(DependencyEdgeSource.NOTION_REFERENCE, commandSlot.captured.source)
    }

    @Test
    fun `동기화는 기존 block을 덮어쓰기 전에 previousText에 직전 값을 보관한다`() {
        every { findProjectDetailById.find(1L, 9L) } returns ProjectDetail(
            id = 1L,
            name = "Project",
            notionRootPageId = "root-page",
            members = emptyList(),
        )
        every { searchCategoriesByProject.search(1L) } returns emptyList()
        every { projectRepository.findById(1L) } returns null
        val document = Document(projectId = 1L, notionPageId = "root-page", title = "Root").also { it.id = 100L }
        val existingBlock = Block(
            document = document,
            notionBlockId = "block-1",
            type = "paragraph",
            text = "before text",
            sortOrder = 0,
        )
        every { documentRepository.findByProjectIdAndNotionPageId(1L, "root-page") } returns document
        every { documentRepository.save(any()) } answers { firstArg() }
        every { blockRepository.findByDocument_IdOrderBySortOrderAsc(100L) } returns listOf(existingBlock)
        every { blockRepository.deleteAll(any<Iterable<Block>>()) } returns Unit
        every { blockRepository.saveAll(any<Iterable<Block>>()) } answers {
            firstArg<Iterable<Block>>().toList()
        }
        every { notionDocumentClient.fetchPage("root-page", null) } returns page("root-page", "Root")
        every { notionDocumentClient.fetchBlockChildren("root-page", null) } returns listOf(
            block(id = "block-1", text = "after text"),
        )

        handler.handle(SyncProjectDocumentsCommand(projectId = 1L, requestedBy = 9L))

        assertEquals("before text", existingBlock.previousText)
        assertEquals("after text", existingBlock.text)
    }

    @Test
    fun `initial sync stores pages discovered through page mentions and their child pages`() {
        every { findProjectDetailById.find(1L, 9L) } returns ProjectDetail(
            id = 1L,
            name = "Project",
            notionRootPageId = "root-page",
            members = emptyList(),
        )
        every { searchCategoriesByProject.search(1L) } returns emptyList()
        every { projectRepository.findById(1L) } returns null
        every { documentRepository.findByProjectIdAndNotionPageId(any(), any()) } returns null

        var nextDocumentId = 100L
        val savedDocuments = mutableListOf<Document>()
        every { documentRepository.save(any()) } answers {
            firstArg<Document>().also {
                it.id = nextDocumentId++
                savedDocuments += it
            }
        }
        every { blockRepository.findByDocument_IdOrderBySortOrderAsc(any()) } returns emptyList()
        every { blockRepository.deleteAll(any<Iterable<Block>>()) } returns Unit
        every { blockRepository.saveAll(any<Iterable<Block>>()) } answers {
            firstArg<Iterable<Block>>().toList()
        }

        every { notionDocumentClient.fetchPage("root-page", null) } returns page("root-page", "Root")
        every { notionDocumentClient.fetchBlockChildren("root-page", null) } returns listOf(
            block(id = "paragraph-1", text = "See linked", linkedPageIds = setOf("linked-page")),
        )
        every { notionDocumentClient.fetchPage("linked-page", null) } returns page("linked-page", "Linked")
        every { notionDocumentClient.fetchBlockChildren("linked-page", null) } returns listOf(
            block(id = "nested-child-page", type = "child_page", childPageTitle = "Nested"),
        )
        every { notionDocumentClient.fetchPage("nested-child-page", null) } returns page("nested-child-page", "Nested")
        every { notionDocumentClient.fetchBlockChildren("nested-child-page", null) } returns emptyList()

        handler.handle(SyncProjectDocumentsCommand(projectId = 1L, requestedBy = 9L))

        assertEquals(listOf("root-page", "linked-page", "nested-child-page"), savedDocuments.map { it.notionPageId })
        val linked = savedDocuments.first { it.notionPageId == "linked-page" }
        assertNull(linked.parentNotionPageId)
        assertNull(linked.parentDocumentId)
        val nested = savedDocuments.first { it.notionPageId == "nested-child-page" }
        assertEquals("linked-page", nested.parentNotionPageId)
        assertEquals(linked.id, nested.parentDocumentId)
    }

    private fun page(id: String, title: String): NotionPage = NotionPage(
        id = id,
        title = title,
        icon = null,
        createdTime = null,
        lastEditedTime = null,
        createdBy = null,
        lastEditedBy = null,
        rawJson = """{"id":"$id"}""",
    )

    private fun block(
        id: String,
        type: String = "paragraph",
        text: String? = null,
        linkedPageIds: Set<String> = emptySet(),
        childPageTitle: String? = null,
        hasChildren: Boolean = false,
    ): NotionBlock = NotionBlock(
        id = id,
        type = type,
        parentType = "page_id",
        parentId = "parent",
        text = text,
        linkedPageIds = linkedPageIds,
        childPageTitle = childPageTitle,
        createdTime = null,
        lastEditedTime = null,
        createdBy = null,
        lastEditedBy = null,
        hasChildren = hasChildren,
        archived = false,
        inTrash = false,
        rawJson = """{"id":"$id"}""",
    )
}
