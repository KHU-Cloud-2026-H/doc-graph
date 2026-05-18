package com.docgraph.backend.validation.command.infra

import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.validation.command.domain.Conflict
import com.docgraph.backend.validation.command.domain.ConflictRepository
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
@Import(SharedPostgresContainer::class, ConflictRepositoryImpl::class)
class ConflictRepositoryImplTest @Autowired constructor(
    private val repository: ConflictRepository,
) {

    @PersistenceContext
    private lateinit var em: EntityManager

    @Test
    fun `findFirstByEdgeIdAndResolvedAtIsNull — 활성 conflict 반환`() {
        val saved = repository.save(newConflict(edgeId = 10L))
        em.flush()

        val found = repository.findFirstByEdgeIdAndResolvedAtIsNull(10L)

        assertNotNull(found)
        assertEquals(saved.id, found.id)
    }

    @Test
    fun `findFirstByEdgeIdAndResolvedAtIsNull — resolved 상태면 null`() {
        val conflict = repository.save(newConflict(edgeId = 11L))
        em.flush()
        conflict.markResolved(OffsetDateTime.now())
        repository.save(conflict)
        em.flush()

        assertNull(repository.findFirstByEdgeIdAndResolvedAtIsNull(11L))
    }

    @Test
    fun `같은 edge_id로 활성 conflict 두 건 — partial UNIQUE 제약 위반`() {
        repository.save(newConflict(edgeId = 12L))
        em.flush()

        assertThrows<DataIntegrityViolationException> {
            repository.save(newConflict(edgeId = 12L))
            em.flush()
        }
    }

    @Test
    fun `resolved 후 같은 edge_id로 새 conflict — 허용`() {
        val first = repository.save(newConflict(edgeId = 13L))
        em.flush()
        first.markResolved(OffsetDateTime.now())
        repository.save(first)
        em.flush()

        val second = repository.save(newConflict(edgeId = 13L))
        em.flush()

        assertNotNull(second.id)
        assertEquals(13L, second.edgeId)
    }

    private fun newConflict(edgeId: Long): Conflict {
        val now = OffsetDateTime.now()
        return Conflict(edgeId = edgeId, firstDetectedAt = now, lastDetectedAt = now)
    }
}
