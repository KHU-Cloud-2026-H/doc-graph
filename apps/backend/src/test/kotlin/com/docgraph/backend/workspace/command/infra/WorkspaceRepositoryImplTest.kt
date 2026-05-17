package com.docgraph.backend.workspace.command.infra

import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.workspace.command.domain.Workspace
import com.docgraph.backend.workspace.command.domain.WorkspaceRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Tag("slice")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SharedPostgresContainer::class, WorkspaceRepositoryImpl::class)
class WorkspaceRepositoryImplTest @Autowired constructor(
    private val repository: WorkspaceRepository,
) {

    @PersistenceContext
    private lateinit var em: EntityManager

    @Test
    fun `save 후 findById로 조회`() {
        val saved = repository.save(newWorkspace(notionWorkspaceId = "nw-1"))
        em.flush()

        val found = repository.findById(saved.id)

        assertNotNull(found)
        assertEquals("nw-1", found.notionWorkspaceId)
        assertEquals("Workspace", found.notionWorkspaceName)
        assertEquals("bot-1", found.notionBotId)
        assertEquals(1L, found.createdBy)
    }

    @Test
    fun `notion_workspace_id UNIQUE 제약 — 중복 시 violation`() {
        repository.save(newWorkspace(notionWorkspaceId = "nw-dup"))
        em.flush()

        assertThrows<DataIntegrityViolationException> {
            repository.save(newWorkspace(notionWorkspaceId = "nw-dup"))
            em.flush()
        }
    }

    @Test
    fun `findByNotionWorkspaceId — 매칭 row 반환`() {
        repository.save(newWorkspace(notionWorkspaceId = "nw-find"))
        em.flush()

        val found = repository.findByNotionWorkspaceId("nw-find")

        assertNotNull(found)
        assertEquals("nw-find", found.notionWorkspaceId)
    }

    @Test
    fun `findByNotionWorkspaceId — 없으면 null`() {
        assertNull(repository.findByNotionWorkspaceId("nw-none"))
    }

    private fun newWorkspace(notionWorkspaceId: String): Workspace {
        return Workspace(
            notionWorkspaceId = notionWorkspaceId,
            notionWorkspaceName = "Workspace",
            notionBotId = "bot-1",
            createdBy = 1L,
            createdAt = OffsetDateTime.now(),
        )
    }
}