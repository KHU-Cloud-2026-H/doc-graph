package com.docgraph.backend.validation.command.domain

import com.docgraph.backend.fixtures.SharedPostgresContainer
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
@Import(SharedPostgresContainer::class)
class ConflictRepositoryTest @Autowired constructor(
    private val repository: ConflictRepository,
) {

    @Test
    fun `findFirstByEdgeIdAndResolvedAtIsNull — 활성 conflict 반환`() {
        val saved = repository.saveAndFlush(newConflict(edgeId = 10L))

        val found = repository.findFirstByEdgeIdAndResolvedAtIsNull(10L)

        assertNotNull(found)
        assertEquals(saved.id, found.id)
    }

    @Test
    fun `findFirstByEdgeIdAndResolvedAtIsNull — resolved 상태면 null`() {
        val conflict = repository.saveAndFlush(newConflict(edgeId = 11L))
        conflict.markResolved(OffsetDateTime.now())
        repository.saveAndFlush(conflict)

        assertNull(repository.findFirstByEdgeIdAndResolvedAtIsNull(11L))
    }

    @Test
    fun `같은 edge_id로 활성 conflict 두 건 — partial UNIQUE 제약 위반`() {
        repository.saveAndFlush(newConflict(edgeId = 12L))

        assertThrows<DataIntegrityViolationException> {
            repository.saveAndFlush(newConflict(edgeId = 12L))
        }
    }

    @Test
    fun `resolved 후 같은 edge_id로 새 conflict — 허용`() {
        val first = repository.saveAndFlush(newConflict(edgeId = 13L))
        first.markResolved(OffsetDateTime.now())
        repository.saveAndFlush(first)

        val second = repository.saveAndFlush(newConflict(edgeId = 13L))

        assertNotNull(second.id)
        assertEquals(13L, second.edgeId)
    }

    private fun newConflict(edgeId: Long): Conflict {
        val now = OffsetDateTime.now()
        return Conflict(edgeId = edgeId, firstDetectedAt = now, lastDetectedAt = now)
    }
}
