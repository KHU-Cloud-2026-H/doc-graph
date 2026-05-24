package com.docgraph.backend.graph.query.infra

import com.docgraph.backend.document.command.domain.Document
import com.docgraph.backend.document.command.domain.DocumentRepository
import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.graph.command.domain.DependencyEdge
import com.docgraph.backend.graph.command.domain.DependencyEdgeSource
import com.docgraph.backend.graph.command.infra.DependencyEdgeJpaRepository
import com.docgraph.backend.project.command.domain.Project
import com.docgraph.backend.project.command.infra.ProjectJpaRepository
import com.docgraph.backend.workspace.command.domain.Workspace
import com.docgraph.backend.workspace.command.infra.WorkspaceJpaRepository
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Tag("slice")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SharedPostgresContainer::class, GraphQueryRepository::class)
class GraphQueryRepositoryTest @Autowired constructor(
    private val queryRepository: GraphQueryRepository,
    private val edgeJpaRepository: DependencyEdgeJpaRepository,
    private val projectJpa: ProjectJpaRepository,
    private val workspaceJpa: WorkspaceJpaRepository,
    private val documentRepository: DocumentRepository,
) {

    @Test
    fun `findEdgeIdsByProjectIdIn — projectId별 edge ids 그룹화 반환`() {
        val (p1, p2) = seedTwoProjects()
        val (s1, t1) = seedDocPair(p1.id, "a")
        val (s2, t2) = seedDocPair(p1.id, "b")
        val (s3, t3) = seedDocPair(p2.id, "c")
        val a = edgeJpaRepository.save(newEdge(p1.id, s1.id, t1.id))
        val b = edgeJpaRepository.save(newEdge(p1.id, s2.id, t2.id))
        val c = edgeJpaRepository.save(newEdge(p2.id, s3.id, t3.id))

        val result = queryRepository.findEdgeIdsByProjectIdIn(listOf(p1.id, p2.id))

        assertEquals(setOf(a.id, b.id), result[p1.id]?.toSet())
        assertEquals(listOf(c.id), result[p2.id])
    }

    @Test
    fun `findEdgeIdsByProjectIdIn — 빈 입력이면 빈 Map`() {
        val (p1, _) = seedTwoProjects()
        val (s, t) = seedDocPair(p1.id, "a")
        edgeJpaRepository.save(newEdge(p1.id, s.id, t.id))

        val result = queryRepository.findEdgeIdsByProjectIdIn(emptyList())

        assertTrue(result.isEmpty())
    }

    @Test
    fun `findEdgeIdsByProjectIdIn — edge 없는 projectId는 Map에 미포함`() {
        val (p1, p2) = seedTwoProjects()
        val (s, t) = seedDocPair(p1.id, "a")
        val a = edgeJpaRepository.save(newEdge(p1.id, s.id, t.id))

        val result = queryRepository.findEdgeIdsByProjectIdIn(listOf(p1.id, p2.id))

        assertEquals(listOf(a.id), result[p1.id])
        assertNull(result[p2.id])
    }

    @Test
    fun `findEdgeIdsByProjectIdIn — 입력에 포함되지 않은 projectId는 제외`() {
        val (p1, p2) = seedTwoProjects()
        val (s, t) = seedDocPair(p1.id, "a")
        edgeJpaRepository.save(newEdge(p1.id, s.id, t.id))

        val result = queryRepository.findEdgeIdsByProjectIdIn(listOf(p2.id))

        assertTrue(result.isEmpty())
    }

    private fun seedTwoProjects(): Pair<Project, Project> {
        val ws = workspaceJpa.save(
            Workspace(
                notionWorkspaceId = "nw-${System.nanoTime()}",
                notionWorkspaceName = "ws",
                notionBotId = "bot-${System.nanoTime()}",
                createdBy = 1L,
            ),
        )
        val p1 = projectJpa.save(Project(workspaceId = ws.id, name = "P1", notionRootPageId = "r1-${ws.id}", createdBy = 1L))
        val p2 = projectJpa.save(Project(workspaceId = ws.id, name = "P2", notionRootPageId = "r2-${ws.id}", createdBy = 1L))
        return p1 to p2
    }

    private fun seedDocPair(projectId: Long, tag: String): Pair<Document, Document> {
        val source = documentRepository.save(
            Document(projectId = projectId, notionPageId = "src-$projectId-$tag", title = "S-$tag"),
        )
        val target = documentRepository.save(
            Document(projectId = projectId, notionPageId = "tgt-$projectId-$tag", title = "T-$tag"),
        )
        return source to target
    }

    private fun newEdge(projectId: Long, sourceDocId: Long, targetDocId: Long): DependencyEdge =
        DependencyEdge(
            projectId = projectId,
            sourceDocumentId = sourceDocId,
            targetDocumentId = targetDocId,
            validationCriterion = "범위 일치 여부",
            source = DependencyEdgeSource.NOTION_REFERENCE,
        )
}
