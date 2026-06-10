package com.docgraph.backend.document.command.infra

import com.docgraph.backend.document.command.domain.Block
import com.docgraph.backend.document.command.domain.BlockRepository
import com.docgraph.backend.document.command.domain.Document
import com.docgraph.backend.document.command.domain.DocumentRepository
import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.project.command.domain.Project
import com.docgraph.backend.project.command.infra.ProjectJpaRepository
import com.docgraph.backend.workspace.command.domain.Workspace
import com.docgraph.backend.workspace.command.infra.WorkspaceJpaRepository
import org.junit.jupiter.api.BeforeEach
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
    private val workspaceJpa: WorkspaceJpaRepository,
    private val projectJpa: ProjectJpaRepository,
) {

    // document.project_id → project FK 충족용 — 서로 다른 두 프로젝트를 시드한다.
    private var p1 = 0L
    private var p2 = 0L

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
            Project(workspaceId = ws.id, name = tag, notionRootPageId = "root-$tag-${System.nanoTime()}", createdBy = 1L),
        ).id
        p1 = project("p1")
        p2 = project("p2")
    }

    @Test
    fun `deleteByDocument_ProjectId — 해당 프로젝트 document의 block만 삭제, 타 프로젝트는 보존`() {
        val target = documentRepository.save(Document(projectId = p1, notionPageId = "p1", title = "P1"))
        blockRepository.save(Block(document = target, notionBlockId = "b1", type = "paragraph", sortOrder = 0))
        val other = documentRepository.save(Document(projectId = p2, notionPageId = "p2", title = "P2"))
        val otherBlock = blockRepository.save(
            Block(document = other, notionBlockId = "b2", type = "paragraph", sortOrder = 0),
        )

        blockRepository.deleteByDocument_ProjectId(p1)

        assertTrue(blockRepository.findByDocument_IdOrderBySortOrderAsc(target.id).isEmpty())
        assertEquals(listOf(otherBlock.id), blockRepository.findByDocument_IdOrderBySortOrderAsc(other.id).map { it.id })
    }

    @Test
    fun `deleteByProjectId — 해당 프로젝트 document만 삭제, 타 프로젝트는 보존`() {
        documentRepository.save(Document(projectId = p1, notionPageId = "p1-a", title = "A"))
        documentRepository.save(Document(projectId = p1, notionPageId = "p1-b", title = "B"))
        val survivor = documentRepository.save(Document(projectId = p2, notionPageId = "p2-a", title = "Other"))

        documentRepository.deleteByProjectId(p1)

        assertTrue(documentRepository.findByProjectId(p1).isEmpty())
        assertEquals(listOf(survivor.id), documentRepository.findByProjectId(p2).map { it.id })
    }
}
