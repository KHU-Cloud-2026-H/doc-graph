package com.docgraph.backend.document.query.infra

import com.docgraph.backend.document.command.domain.Block
import com.docgraph.backend.document.command.domain.BlockRepository
import com.docgraph.backend.document.command.domain.Document
import com.docgraph.backend.document.command.domain.DocumentRepository
import com.docgraph.backend.document.query.application.DocumentReference
import com.docgraph.backend.document.query.application.DocumentStats
import com.docgraph.backend.document.query.application.DocumentType
import com.docgraph.backend.document.query.application.IconResponse
import com.docgraph.backend.document.query.application.IconType
import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.project.command.domain.Project
import com.docgraph.backend.project.command.infra.ProjectJpaRepository
import com.docgraph.backend.project.query.application.AssignedDocumentType
import com.docgraph.backend.workspace.command.domain.Workspace
import com.docgraph.backend.workspace.command.infra.WorkspaceJpaRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Tag("slice")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SharedPostgresContainer::class, DocumentQueryRepository::class)
class DocumentQueryRepositoryTest @Autowired constructor(
    private val queryRepository: DocumentQueryRepository,
    private val documentRepository: DocumentRepository,
    private val blockRepository: BlockRepository,
    private val workspaceJpa: WorkspaceJpaRepository,
    private val projectJpa: ProjectJpaRepository,
) {

    // document.project_id → project FK 충족용. 테스트가 쓰던 projectId 리터럴(1·2·7·8)을 실제 시드 id로 대체.
    private var p1 = 0L
    private var p2 = 0L
    private var p7 = 0L
    private var p8 = 0L

    @BeforeEach
    fun seedProjects() {
        val ws = workspaceJpa.save(
            Workspace(
                notionWorkspaceId = "ws-${System.nanoTime()}",
                notionWorkspaceName = "ws",
                notionBotId = "bot-${System.nanoTime()}",
                createdBy = 1L,
            ),
        )
        fun project(tag: String) = projectJpa.save(
            Project(
                workspaceId = ws.id,
                name = tag,
                notionRootPageId = "root-$tag-${System.nanoTime()}",
                createdBy = 1L,
            ),
        ).id
        p1 = project("p1")
        p2 = project("p2")
        p7 = project("p7")
        p8 = project("p8")
    }

    @Test
    fun `findDetail — 존재하는 document를 sortOrder 순 blocks와 함께 반환`() {
        val document = documentRepository.save(
            Document(
                projectId = p1,
                notionPageId = "page-1",
                title = "Page",
                type = DocumentType.PLANNING,
            ),
        )
        blockRepository.save(
            Block(
                document = document,
                notionBlockId = "block-2",
                type = "paragraph",
                sortOrder = 2,
                text = "second",
            ),
        )
        blockRepository.save(
            Block(
                document = document,
                notionBlockId = "block-1",
                type = "paragraph",
                sortOrder = 1,
                text = "first",
            ),
        )

        val detail = queryRepository.findDetail(document.id)

        assertNotNull(detail)
        assertEquals(document.id, detail!!.id)
        assertEquals("Page", detail.title)
        assertEquals(DocumentType.PLANNING, detail.type)
        assertEquals(2, detail.blocks.size)
        assertEquals("block-1", detail.blocks[0].blockId)
        assertEquals("block-2", detail.blocks[1].blockId)
    }

    @Test
    fun `findDetail — 존재하지 않으면 null`() {
        val detail = queryRepository.findDetail(99999L)
        assertNull(detail)
    }

    @Test
    fun `findDetail — Block parentType이 block_id일 때만 parentBlockId 채움, page_id면 null`() {
        val document = documentRepository.save(
            Document(projectId = p1, notionPageId = "page-1", title = "Page"),
        )
        blockRepository.save(
            Block(
                document = document,
                notionBlockId = "child-block",
                type = "paragraph",
                sortOrder = 1,
                parentType = "block_id",
                parentId = "parent-block",
            ),
        )
        blockRepository.save(
            Block(
                document = document,
                notionBlockId = "root-block",
                type = "paragraph",
                sortOrder = 2,
                parentType = "page_id",
                parentId = "page-1",
            ),
        )

        val detail = queryRepository.findDetail(document.id)
        assertNotNull(detail)

        val childBlock = detail!!.blocks.first { it.blockId == "child-block" }
        val rootBlock = detail.blocks.first { it.blockId == "root-block" }
        assertEquals("parent-block", childBlock.parentBlockId)
        assertNull(rootBlock.parentBlockId)
    }

    @Test
    fun `findDetail — notionLastEditedAt이 그대로 매핑 (non-null)`() {
        val edited = OffsetDateTime.parse("2026-05-15T10:30:00Z")
        val document = documentRepository.save(
            Document(projectId = p1, notionPageId = "page-edited", title = "Edited", notionLastEditedAt = edited),
        )

        val detail = queryRepository.findDetail(document.id)

        assertNotNull(detail)
        assertEquals(edited, detail!!.notionLastEditedAt)
    }

    @Test
    fun `findDetail — notionLastEditedAt 미세팅이면 null`() {
        val document = documentRepository.save(
            Document(projectId = p1, notionPageId = "page-unset", title = "Unset"),
        )

        val detail = queryRepository.findDetail(document.id)

        assertNotNull(detail)
        assertNull(detail!!.notionLastEditedAt)
    }

    @Test
    fun `findNotionLastEditedById — 저장된 Notion user id를 그대로 반환`() {
        val document = documentRepository.save(
            Document(
                projectId = p1,
                notionPageId = "page-editor",
                title = "Edited",
                notionLastEditedBy = "notion-user-1",
            ),
        )

        assertEquals("notion-user-1", queryRepository.findNotionLastEditedById(document.id))
    }

    @Test
    fun `findNotionLastEditedById — 미세팅이면 null, 없는 document면 null`() {
        val document = documentRepository.save(
            Document(projectId = p1, notionPageId = "page-unset-editor", title = "Unset"),
        )

        assertNull(queryRepository.findNotionLastEditedById(document.id))
        assertNull(queryRepository.findNotionLastEditedById(99999L))
    }

    @Test
    fun `findDetail — type이 null인 document도 정상 반환`() {
        val document = documentRepository.save(
            Document(projectId = p1, notionPageId = "page-1", title = "Page", type = null),
        )

        val detail = queryRepository.findDetail(document.id)

        assertNotNull(detail)
        assertNull(detail!!.type)
    }

    @Test
    fun `searchSummariesByProject — 프로젝트의 document를 페이지네이션으로 반환`() {
        repeat(3) { i ->
            documentRepository.save(
                Document(
                    projectId = p1,
                    notionPageId = "page-$i",
                    title = "Page $i",
                    type = DocumentType.PLANNING,
                ),
            )
        }

        val page0 = queryRepository.searchSummariesByProject(p1, PageRequest.of(0, 2))
        val page1 = queryRepository.searchSummariesByProject(p1, PageRequest.of(1, 2))

        assertEquals(3L, page0.totalElements)
        assertEquals(2, page0.totalPages)
        assertEquals(2, page0.content.size)
        assertEquals(1, page1.content.size)
    }

    @Test
    fun `searchSummariesByProject — 다른 프로젝트의 document는 제외`() {
        documentRepository.save(Document(projectId = p1, notionPageId = "p1-a", title = "A"))
        documentRepository.save(Document(projectId = p1, notionPageId = "p1-b", title = "B"))
        documentRepository.save(Document(projectId = p2, notionPageId = "p2-a", title = "Other"))

        val result = queryRepository.searchSummariesByProject(p1, PageRequest.of(0, 10))

        assertEquals(2L, result.totalElements)
        assertTrue(result.content.all { it.notionPageId.startsWith("p1-") })
    }

    @Test
    fun `searchSummariesByProject — 빈 프로젝트는 빈 페이지`() {
        val result = queryRepository.searchSummariesByProject(999L, PageRequest.of(0, 10))

        assertEquals(0L, result.totalElements)
        assertTrue(result.content.isEmpty())
    }

    @Test
    fun `findDetail — parentDocumentId가 그대로 매핑 (non-null)`() {
        val parent = documentRepository.save(
            Document(projectId = p1, notionPageId = "parent-page", title = "Parent"),
        )
        val child = documentRepository.save(
            Document(
                projectId = p1,
                notionPageId = "child-page",
                title = "Child",
                parentDocumentId = parent.id,
            ),
        )

        val detail = queryRepository.findDetail(child.id)

        assertNotNull(detail)
        assertEquals(parent.id, detail!!.parentDocumentId)
    }

    @Test
    fun `findDetail — parentDocumentId 미세팅이면 null (루트 또는 프로젝트 경계 밖)`() {
        val document = documentRepository.save(
            Document(projectId = p1, notionPageId = "root-page", title = "Root"),
        )

        val detail = queryRepository.findDetail(document.id)

        assertNotNull(detail)
        assertNull(detail!!.parentDocumentId)
    }

    @Test
    fun `searchSummariesByProject — parentDocumentId가 summary에 매핑`() {
        val parent = documentRepository.save(
            Document(projectId = p1, notionPageId = "parent-page", title = "Parent"),
        )
        val child = documentRepository.save(
            Document(
                projectId = p1,
                notionPageId = "child-page",
                title = "Child",
                parentDocumentId = parent.id,
            ),
        )

        val result = queryRepository.searchSummariesByProject(p1, PageRequest.of(0, 10))

        val parentSummary = result.content.first { it.id == parent.id }
        val childSummary = result.content.first { it.id == child.id }
        assertNull(parentSummary.parentDocumentId)
        assertEquals(parent.id, childSummary.parentDocumentId)
    }

    @Test
    fun `searchNodesByProject — 프로젝트 내 document를 DocumentNodeData로 매핑`() {
        val doc = documentRepository.save(
            Document(
                projectId = p1,
                notionPageId = "page-1",
                title = "Node",
                type = DocumentType.PLANNING,
                iconType = IconType.EMOJI,
                iconValue = "📄",
                assigneeMemberId = 7L,
            ),
        )

        val result = queryRepository.searchNodesByProject(p1)

        assertEquals(1, result.size)
        val node = result.first()
        assertEquals(doc.id, node.id)
        assertEquals("Node", node.title)
        assertEquals(DocumentType.PLANNING, node.type)
        assertEquals(7L, node.assigneeMemberId)
        assertEquals(IconResponse(type = IconType.EMOJI, value = "📄"), node.icon)
    }

    @Test
    fun `searchNodesByProject — type과 assigneeMemberId가 null인 document도 그대로 매핑`() {
        documentRepository.save(
            Document(projectId = p1, notionPageId = "page-null", title = "Null"),
        )

        val result = queryRepository.searchNodesByProject(p1)

        assertEquals(1, result.size)
        assertNull(result.first().type)
        assertNull(result.first().assigneeMemberId)
        assertNull(result.first().icon)
    }

    @Test
    fun `searchNodesByProject — 다른 프로젝트 document는 제외, 빈 프로젝트는 빈 리스트`() {
        documentRepository.save(Document(projectId = p1, notionPageId = "p1-a", title = "A"))
        documentRepository.save(Document(projectId = p2, notionPageId = "p2-a", title = "Other"))

        val nodes = queryRepository.searchNodesByProject(p1)
        val empty = queryRepository.searchNodesByProject(999L)

        assertEquals(1, nodes.size)
        assertEquals("A", nodes.first().title)
        assertTrue(empty.isEmpty())
    }

    @Test
    fun `searchIdsByAssignee — 담당자가 일치하는 document id 목록 반환`() {
        val a = documentRepository.save(
            Document(projectId = p1, notionPageId = "a", title = "A", assigneeMemberId = 7L),
        )
        val b = documentRepository.save(
            Document(projectId = p2, notionPageId = "b", title = "B", assigneeMemberId = 7L),
        )
        documentRepository.save(
            Document(projectId = p1, notionPageId = "c", title = "C", assigneeMemberId = 99L),
        )
        documentRepository.save(Document(projectId = p1, notionPageId = "d", title = "D"))

        val ids = queryRepository.searchIdsByAssignee(7L).toSet()

        assertEquals(setOf(a.id, b.id), ids)
    }

    @Test
    fun `searchIdsByAssignee — 매칭 없으면 빈 리스트`() {
        documentRepository.save(
            Document(projectId = p1, notionPageId = "a", title = "A", assigneeMemberId = 7L),
        )

        val ids = queryRepository.searchIdsByAssignee(999L)

        assertTrue(ids.isEmpty())
    }

    @Test
    fun `findReferencesByIds — 매칭되는 document를 id projectId title type으로 반환`() {
        val a = documentRepository.save(
            Document(projectId = p7, notionPageId = "a", title = "Alpha", type = DocumentType.REQUIREMENTS),
        )
        val b = documentRepository.save(
            Document(projectId = p8, notionPageId = "b", title = "Beta", type = null),
        )

        val refs = queryRepository.findReferencesByIds(listOf(a.id, b.id)).toSet()

        assertEquals(
            setOf(
                DocumentReference(a.id, p7, "Alpha", DocumentType.REQUIREMENTS, null),
                DocumentReference(b.id, p8, "Beta", null, null),
            ),
            refs,
        )
    }

    @Test
    fun `findReferencesByIds — 일부 id만 매칭되면 매칭된 것만 반환`() {
        val a = documentRepository.save(
            Document(projectId = p1, notionPageId = "a", title = "Only", type = DocumentType.PLANNING),
        )

        val refs = queryRepository.findReferencesByIds(listOf(a.id, 99999L))

        assertEquals(listOf(DocumentReference(a.id, p1, "Only", DocumentType.PLANNING, null)), refs)
    }

    @Test
    fun `findReferencesByIds — 입력 빈 리스트면 빈 리스트`() {
        val refs = queryRepository.findReferencesByIds(emptyList())
        assertTrue(refs.isEmpty())
    }

    @Test
    fun `findReferencesByIds — 매칭 없으면 빈 리스트`() {
        val refs = queryRepository.findReferencesByIds(listOf(99999L, 88888L))
        assertTrue(refs.isEmpty())
    }

    @Test
    fun `searchIdsByProjectAndTypes — 단일 매핑에 해당하는 document id 반환`() {
        val match = documentRepository.save(
            Document(projectId = p1, notionPageId = "m", title = "M", type = DocumentType.REQUIREMENTS),
        )
        documentRepository.save(
            Document(projectId = p1, notionPageId = "other", title = "O", type = DocumentType.PLANNING),
        )

        val ids = queryRepository.searchIdsByProjectAndTypes(
            listOf(AssignedDocumentType(p1, DocumentType.REQUIREMENTS)),
        )

        assertEquals(listOf(match.id), ids)
    }

    @Test
    fun `searchIdsByProjectAndTypes — 여러 (projectId,type) 묶음 매칭을 union으로 반환`() {
        val p1Req = documentRepository.save(
            Document(projectId = p1, notionPageId = "p1-req", title = "A", type = DocumentType.REQUIREMENTS),
        )
        val p2Plan = documentRepository.save(
            Document(projectId = p2, notionPageId = "p2-plan", title = "B", type = DocumentType.PLANNING),
        )
        documentRepository.save(
            Document(projectId = p1, notionPageId = "p1-plan", title = "C", type = DocumentType.PLANNING),
        )
        documentRepository.save(
            Document(projectId = p2, notionPageId = "p2-req", title = "D", type = DocumentType.REQUIREMENTS),
        )

        val ids = queryRepository.searchIdsByProjectAndTypes(
            listOf(
                AssignedDocumentType(p1, DocumentType.REQUIREMENTS),
                AssignedDocumentType(p2, DocumentType.PLANNING),
            ),
        ).toSet()

        assertEquals(setOf(p1Req.id, p2Plan.id), ids)
    }

    @Test
    fun `searchIdsByProjectAndTypes — type이 일치 안 하면 제외`() {
        documentRepository.save(
            Document(projectId = p1, notionPageId = "x", title = "X", type = DocumentType.PLANNING),
        )

        val ids = queryRepository.searchIdsByProjectAndTypes(
            listOf(AssignedDocumentType(p1, DocumentType.REQUIREMENTS)),
        )

        assertTrue(ids.isEmpty())
    }

    @Test
    fun `searchIdsByProjectAndTypes — projectId가 일치 안 하면 제외`() {
        documentRepository.save(
            Document(projectId = p2, notionPageId = "x", title = "X", type = DocumentType.REQUIREMENTS),
        )

        val ids = queryRepository.searchIdsByProjectAndTypes(
            listOf(AssignedDocumentType(p1, DocumentType.REQUIREMENTS)),
        )

        assertTrue(ids.isEmpty())
    }

    @Test
    fun `searchIdsByProjectAndTypes — type이 null인 document는 매핑 매칭 안 됨`() {
        documentRepository.save(
            Document(projectId = p1, notionPageId = "untyped", title = "U", type = null),
        )

        val ids = queryRepository.searchIdsByProjectAndTypes(
            listOf(AssignedDocumentType(p1, DocumentType.REQUIREMENTS)),
        )

        assertTrue(ids.isEmpty())
    }

    @Test
    fun `searchIdsByProjectAndTypes — 입력이 빈 리스트면 빈 리스트 반환`() {
        documentRepository.save(
            Document(projectId = p1, notionPageId = "x", title = "X", type = DocumentType.REQUIREMENTS),
        )

        val ids = queryRepository.searchIdsByProjectAndTypes(emptyList())

        assertTrue(ids.isEmpty())
    }

    @Test
    fun `searchUnassignedIdsByProject — 프로젝트 내 담당자가 null인 document id 목록 반환`() {
        val a = documentRepository.save(Document(projectId = p1, notionPageId = "a", title = "A"))
        val b = documentRepository.save(Document(projectId = p1, notionPageId = "b", title = "B"))
        documentRepository.save(
            Document(projectId = p1, notionPageId = "c", title = "C", assigneeMemberId = 7L),
        )
        documentRepository.save(Document(projectId = p2, notionPageId = "d", title = "D"))

        val ids = queryRepository.searchUnassignedIdsByProject(p1).toSet()

        assertEquals(setOf(a.id, b.id), ids)
    }

    @Test
    fun `searchUnassignedIdsByProject — 매칭 없으면 빈 리스트`() {
        documentRepository.save(
            Document(projectId = p1, notionPageId = "a", title = "A", assigneeMemberId = 7L),
        )

        val ids = queryRepository.searchUnassignedIdsByProject(p1)

        assertTrue(ids.isEmpty())
    }

    @Test
    fun `searchStatsByProjectIds — projectId별 document count와 notionLastEditedAt MAX 반환`() {
        val t1 = OffsetDateTime.parse("2026-05-01T10:00:00Z")
        val t2 = OffsetDateTime.parse("2026-05-20T10:00:00Z")
        val t3 = OffsetDateTime.parse("2026-05-15T10:00:00Z")
        documentRepository.save(Document(projectId = p1, notionPageId = "a", title = "A", notionLastEditedAt = t1))
        documentRepository.save(Document(projectId = p1, notionPageId = "b", title = "B", notionLastEditedAt = t2))
        documentRepository.save(Document(projectId = p2, notionPageId = "c", title = "C", notionLastEditedAt = t3))

        val stats = queryRepository.searchStatsByProjectIds(listOf(p1, p2))

        assertEquals(DocumentStats(2L, t2), stats[p1])
        assertEquals(DocumentStats(1L, t3), stats[p2])
    }

    @Test
    fun `searchStatsByProjectIds — 입력 빈 리스트면 빈 Map`() {
        documentRepository.save(Document(projectId = p1, notionPageId = "a", title = "A"))

        val stats = queryRepository.searchStatsByProjectIds(emptyList())

        assertTrue(stats.isEmpty())
    }

    @Test
    fun `searchStatsByProjectIds — document 없는 projectId는 Map에 미포함`() {
        documentRepository.save(Document(projectId = p1, notionPageId = "a", title = "A"))

        val stats = queryRepository.searchStatsByProjectIds(listOf(p1, 999L))

        assertEquals(1, stats.size)
        assertEquals(DocumentStats(1L, null), stats[p1])
        assertNull(stats[999L])
    }

    @Test
    fun `searchStatsByProjectIds — notionLastEditedAt이 모두 null인 projectId는 lastChangedAt null`() {
        documentRepository.save(Document(projectId = p1, notionPageId = "a", title = "A"))
        documentRepository.save(Document(projectId = p1, notionPageId = "b", title = "B"))

        val stats = queryRepository.searchStatsByProjectIds(listOf(p1))

        assertEquals(DocumentStats(2L, null), stats[p1])
    }

    @Test
    fun `searchStatsByProjectIds — 다른 projectId의 document는 제외`() {
        documentRepository.save(Document(projectId = p1, notionPageId = "a", title = "A"))
        documentRepository.save(Document(projectId = p2, notionPageId = "b", title = "B"))

        val stats = queryRepository.searchStatsByProjectIds(listOf(p1))

        assertEquals(DocumentStats(1L, null), stats[p1])
        assertNull(stats[p2])
    }

    @Test
    fun `searchStatsByProjectIds — notionLastEditedAt 일부만 null이면 non-null 중 MAX`() {
        val t = OffsetDateTime.parse("2026-05-10T10:00:00Z")
        documentRepository.save(Document(projectId = p1, notionPageId = "a", title = "A"))
        documentRepository.save(Document(projectId = p1, notionPageId = "b", title = "B", notionLastEditedAt = t))

        val stats = queryRepository.searchStatsByProjectIds(listOf(p1))

        assertEquals(DocumentStats(2L, t), stats[p1])
    }
}