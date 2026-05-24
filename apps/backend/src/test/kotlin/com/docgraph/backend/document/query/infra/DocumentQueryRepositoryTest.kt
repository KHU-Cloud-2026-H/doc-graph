package com.docgraph.backend.document.query.infra

import com.docgraph.backend.document.command.domain.Block
import com.docgraph.backend.document.command.domain.BlockRepository
import com.docgraph.backend.document.command.domain.Document
import com.docgraph.backend.document.command.domain.DocumentRepository
import com.docgraph.backend.document.query.application.DocumentType
import com.docgraph.backend.fixtures.SharedPostgresContainer
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
) {

    @Test
    fun `findDetail — 존재하는 document를 sortOrder 순 blocks와 함께 반환`() {
        val document = documentRepository.save(
            Document(
                projectId = 1L,
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
            Document(projectId = 1L, notionPageId = "page-1", title = "Page"),
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
            Document(projectId = 1L, notionPageId = "page-edited", title = "Edited", notionLastEditedAt = edited),
        )

        val detail = queryRepository.findDetail(document.id)

        assertNotNull(detail)
        assertEquals(edited, detail!!.notionLastEditedAt)
    }

    @Test
    fun `findDetail — notionLastEditedAt 미세팅이면 null`() {
        val document = documentRepository.save(
            Document(projectId = 1L, notionPageId = "page-unset", title = "Unset"),
        )

        val detail = queryRepository.findDetail(document.id)

        assertNotNull(detail)
        assertNull(detail!!.notionLastEditedAt)
    }

    @Test
    fun `findDetail — type이 null인 document도 정상 반환`() {
        val document = documentRepository.save(
            Document(projectId = 1L, notionPageId = "page-1", title = "Page", type = null),
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
                    projectId = 1L,
                    notionPageId = "page-$i",
                    title = "Page $i",
                    type = DocumentType.PLANNING,
                ),
            )
        }

        val page0 = queryRepository.searchSummariesByProject(1L, PageRequest.of(0, 2))
        val page1 = queryRepository.searchSummariesByProject(1L, PageRequest.of(1, 2))

        assertEquals(3L, page0.totalElements)
        assertEquals(2, page0.totalPages)
        assertEquals(2, page0.content.size)
        assertEquals(1, page1.content.size)
    }

    @Test
    fun `searchSummariesByProject — 다른 프로젝트의 document는 제외`() {
        documentRepository.save(Document(projectId = 1L, notionPageId = "p1-a", title = "A"))
        documentRepository.save(Document(projectId = 1L, notionPageId = "p1-b", title = "B"))
        documentRepository.save(Document(projectId = 2L, notionPageId = "p2-a", title = "Other"))

        val result = queryRepository.searchSummariesByProject(1L, PageRequest.of(0, 10))

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
            Document(projectId = 1L, notionPageId = "parent-page", title = "Parent"),
        )
        val child = documentRepository.save(
            Document(
                projectId = 1L,
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
            Document(projectId = 1L, notionPageId = "root-page", title = "Root"),
        )

        val detail = queryRepository.findDetail(document.id)

        assertNotNull(detail)
        assertNull(detail!!.parentDocumentId)
    }

    @Test
    fun `searchSummariesByProject — parentDocumentId가 summary에 매핑`() {
        val parent = documentRepository.save(
            Document(projectId = 1L, notionPageId = "parent-page", title = "Parent"),
        )
        val child = documentRepository.save(
            Document(
                projectId = 1L,
                notionPageId = "child-page",
                title = "Child",
                parentDocumentId = parent.id,
            ),
        )

        val result = queryRepository.searchSummariesByProject(1L, PageRequest.of(0, 10))

        val parentSummary = result.content.first { it.id == parent.id }
        val childSummary = result.content.first { it.id == child.id }
        assertNull(parentSummary.parentDocumentId)
        assertEquals(parent.id, childSummary.parentDocumentId)
    }

    @Test
    fun `searchNodesByProject — 프로젝트 내 document를 DocumentNodeData로 매핑`() {
        val doc = documentRepository.save(
            Document(
                projectId = 1L,
                notionPageId = "page-1",
                title = "Node",
                type = DocumentType.PLANNING,
                assigneeMemberId = 7L,
            ),
        )

        val result = queryRepository.searchNodesByProject(1L)

        assertEquals(1, result.size)
        val node = result.first()
        assertEquals(doc.id, node.id)
        assertEquals("Node", node.title)
        assertEquals(DocumentType.PLANNING, node.type)
        assertEquals(7L, node.assigneeMemberId)
    }

    @Test
    fun `searchNodesByProject — type과 assigneeMemberId가 null인 document도 그대로 매핑`() {
        documentRepository.save(
            Document(projectId = 1L, notionPageId = "page-null", title = "Null"),
        )

        val result = queryRepository.searchNodesByProject(1L)

        assertEquals(1, result.size)
        assertNull(result.first().type)
        assertNull(result.first().assigneeMemberId)
    }

    @Test
    fun `searchNodesByProject — 다른 프로젝트 document는 제외, 빈 프로젝트는 빈 리스트`() {
        documentRepository.save(Document(projectId = 1L, notionPageId = "p1-a", title = "A"))
        documentRepository.save(Document(projectId = 2L, notionPageId = "p2-a", title = "Other"))

        val p1 = queryRepository.searchNodesByProject(1L)
        val empty = queryRepository.searchNodesByProject(999L)

        assertEquals(1, p1.size)
        assertEquals("A", p1.first().title)
        assertTrue(empty.isEmpty())
    }

    @Test
    fun `searchIdsByAssignee — 담당자가 일치하는 document id 목록 반환`() {
        val a = documentRepository.save(
            Document(projectId = 1L, notionPageId = "a", title = "A", assigneeMemberId = 7L),
        )
        val b = documentRepository.save(
            Document(projectId = 2L, notionPageId = "b", title = "B", assigneeMemberId = 7L),
        )
        documentRepository.save(
            Document(projectId = 1L, notionPageId = "c", title = "C", assigneeMemberId = 99L),
        )
        documentRepository.save(Document(projectId = 1L, notionPageId = "d", title = "D"))

        val ids = queryRepository.searchIdsByAssignee(7L).toSet()

        assertEquals(setOf(a.id, b.id), ids)
    }

    @Test
    fun `searchIdsByAssignee — 매칭 없으면 빈 리스트`() {
        documentRepository.save(
            Document(projectId = 1L, notionPageId = "a", title = "A", assigneeMemberId = 7L),
        )

        val ids = queryRepository.searchIdsByAssignee(999L)

        assertTrue(ids.isEmpty())
    }

    @Test
    fun `searchUnassignedIdsByProject — 프로젝트 내 담당자가 null인 document id 목록 반환`() {
        val a = documentRepository.save(Document(projectId = 1L, notionPageId = "a", title = "A"))
        val b = documentRepository.save(Document(projectId = 1L, notionPageId = "b", title = "B"))
        documentRepository.save(
            Document(projectId = 1L, notionPageId = "c", title = "C", assigneeMemberId = 7L),
        )
        documentRepository.save(Document(projectId = 2L, notionPageId = "d", title = "D"))

        val ids = queryRepository.searchUnassignedIdsByProject(1L).toSet()

        assertEquals(setOf(a.id, b.id), ids)
    }

    @Test
    fun `searchUnassignedIdsByProject — 매칭 없으면 빈 리스트`() {
        documentRepository.save(
            Document(projectId = 1L, notionPageId = "a", title = "A", assigneeMemberId = 7L),
        )

        val ids = queryRepository.searchUnassignedIdsByProject(1L)

        assertTrue(ids.isEmpty())
    }
}