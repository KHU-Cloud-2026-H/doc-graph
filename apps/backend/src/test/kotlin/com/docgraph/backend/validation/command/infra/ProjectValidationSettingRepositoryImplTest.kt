package com.docgraph.backend.validation.command.infra

import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.validation.command.domain.ProjectValidationSetting
import com.docgraph.backend.validation.command.domain.ProjectValidationSettingRepository
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Tag("slice")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SharedPostgresContainer::class, ProjectValidationSettingRepositoryImpl::class)
class ProjectValidationSettingRepositoryImplTest @Autowired constructor(
    private val repository: ProjectValidationSettingRepository,
) {

    @PersistenceContext
    private lateinit var em: EntityManager

    @Test
    fun `save 후 findByProjectId로 조회 — enabled 값 보존`() {
        repository.save(newSetting(projectId = 10L, enabled = false))
        em.flush()

        val found = repository.findByProjectId(10L)

        assertNotNull(found)
        assertEquals(10L, found.projectId)
        assertFalse(found.enabled)
    }

    @Test
    fun `project_id UNIQUE 제약 — 중복 시 violation`() {
        repository.save(newSetting(projectId = 20L, enabled = false))
        em.flush()

        assertThrows<DataIntegrityViolationException> {
            repository.save(newSetting(projectId = 20L, enabled = true))
            em.flush()
        }
    }

    @Test
    fun `findByProjectId — 없으면 null`() {
        assertNull(repository.findByProjectId(99999L))
    }

    @Test
    fun `changeEnabled 후 save — 갱신 반영`() {
        val saved = repository.save(newSetting(projectId = 25L, enabled = false))
        em.flush()

        saved.changeEnabled(true, OffsetDateTime.now())
        repository.save(saved)
        em.flush()
        em.clear()

        assertEquals(true, repository.findByProjectId(25L)?.enabled)
    }

    @Test
    fun `deleteByProjectId — 해당 row 삭제`() {
        repository.save(newSetting(projectId = 30L, enabled = false))
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

    private fun newSetting(projectId: Long, enabled: Boolean): ProjectValidationSetting =
        ProjectValidationSetting(projectId = projectId, enabled = enabled, createdAt = OffsetDateTime.now())
}
