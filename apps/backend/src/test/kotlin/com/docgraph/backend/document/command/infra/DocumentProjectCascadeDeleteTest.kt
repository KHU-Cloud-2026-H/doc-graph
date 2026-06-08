package com.docgraph.backend.document.command.infra

import com.docgraph.backend.document.command.domain.Block
import com.docgraph.backend.document.command.domain.BlockRepository
import com.docgraph.backend.document.command.domain.Document
import com.docgraph.backend.document.command.domain.DocumentRepository
import com.docgraph.backend.fixtures.SharedPostgresContainer
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Tag("slice")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SharedPostgresContainer::class)
class DocumentProjectCascadeDeleteTest @Autowired constructor(
    private val documentRepository: DocumentRepository,
    private val blockRepository: BlockRepository,
) {

    @Test
    fun `deleteByDocument_ProjectId — 해당 프로젝트 document의 block만 삭제, 타 프로젝트는 보존`() {
        val target = documentRepository.save(Document(projectId = 1L, notionPageId = "p1", title = "P1"))
        blockRepository.save(Block(document = target, notionBlockId = "b1", type = "paragraph", sortOrder = 0))
        val other = documentRepository.save(Document(projectId = 2L, notionPageId = "p2", title = "P2"))
        val otherBlock = blockRepository.save(
            Block(document = other, notionBlockId = "b2", type = "paragraph", sortOrder = 0),
        )

        blockRepository.deleteByDocument_ProjectId(1L)

        assertTrue(blockRepository.findByDocument_IdOrderBySortOrderAsc(target.id).isEmpty())
        assertEquals(listOf(otherBlock.id), blockRepository.findByDocument_IdOrderBySortOrderAsc(other.id).map { it.id })
    }

    @Test
    fun `deleteByProjectId — 해당 프로젝트 document만 삭제, 타 프로젝트는 보존`() {
        documentRepository.save(Document(projectId = 1L, notionPageId = "p1-a", title = "A"))
        documentRepository.save(Document(projectId = 1L, notionPageId = "p1-b", title = "B"))
        val survivor = documentRepository.save(Document(projectId = 2L, notionPageId = "p2-a", title = "Other"))

        documentRepository.deleteByProjectId(1L)

        assertTrue(documentRepository.findByProjectId(1L).isEmpty())
        assertEquals(listOf(survivor.id), documentRepository.findByProjectId(2L).map { it.id })
    }
}
