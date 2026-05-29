package com.docgraph.backend.notification.command.infra

import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.notification.command.domain.ProjectWebhook
import com.docgraph.backend.notification.command.domain.ProjectWebhookRepository
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
@Import(SharedPostgresContainer::class, ProjectWebhookRepositoryImpl::class)
class ProjectWebhookRepositoryImplTest @Autowired constructor(
    private val repository: ProjectWebhookRepository,
) {

    @PersistenceContext
    private lateinit var em: EntityManager

    @Test
    fun `save 후 findByProjectId로 조회`() {
        repository.save(newWebhook(projectId = 10L, url = "https://hooks.slack.com/x"))
        em.flush()

        val found = repository.findByProjectId(10L)

        assertNotNull(found)
        assertEquals(10L, found.projectId)
        assertEquals("https://hooks.slack.com/x", found.url)
    }

    @Test
    fun `project_id UNIQUE 제약 — 중복 시 violation`() {
        repository.save(newWebhook(projectId = 20L, url = "https://a"))
        em.flush()

        assertThrows<DataIntegrityViolationException> {
            repository.save(newWebhook(projectId = 20L, url = "https://b"))
            em.flush()
        }
    }

    @Test
    fun `findByProjectId — 없으면 null`() {
        assertNull(repository.findByProjectId(99999L))
    }

    @Test
    fun `deleteByProjectId — 해당 row 삭제`() {
        repository.save(newWebhook(projectId = 30L, url = "https://x"))
        em.flush()

        repository.deleteByProjectId(30L)
        em.flush()
        em.clear()

        assertNull(repository.findByProjectId(30L))
    }

    @Test
    fun `deleteByProjectId — 없는 projectId no-op`() {
        repository.deleteByProjectId(99999L)
        em.flush()
    }

    private fun newWebhook(projectId: Long, url: String): ProjectWebhook =
        ProjectWebhook(projectId = projectId, url = url, createdAt = OffsetDateTime.now())
}
