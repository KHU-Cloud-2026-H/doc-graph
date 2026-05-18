package com.docgraph.backend.validation.command.infra

import com.docgraph.backend.event.OutboxStatus
import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.validation.command.domain.ValidationTask
import com.docgraph.backend.validation.command.domain.ValidationTaskRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import java.time.Duration
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Tag("slice")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SharedPostgresContainer::class, ValidationTaskRepositoryImpl::class)
class ValidationTaskRepositoryImplTest @Autowired constructor(
    private val repository: ValidationTaskRepository,
) {

    @PersistenceContext
    private lateinit var em: EntityManager

    @Test
    fun `findByValidationPairId — 존재하면 task 반환`() {
        val pairId = UUID.randomUUID()
        val saved = repository.save(ValidationTask(validationPairId = pairId, edgeId = 100L))
        em.flush()

        val found = repository.findByValidationPairId(pairId)

        assertNotNull(found)
        assertEquals(saved.id, found.id)
    }

    @Test
    fun `findByValidationPairId — 없으면 null`() {
        val found = repository.findByValidationPairId(UUID.randomUUID())
        assertNull(found)
    }

    @Test
    fun `findStale — lastAttemptAt이 cutoff 이전인 PENDING만 반환`() {
        val now = OffsetDateTime.now()
        val staleTime = now.minus(Duration.ofMinutes(10))
        val freshTime = now.minus(Duration.ofMinutes(1))
        val cutoff = now.minus(Duration.ofMinutes(5))

        val stalePending = repository.save(
            ValidationTask(validationPairId = UUID.randomUUID(), edgeId = 1L, lastAttemptAt = staleTime, attempts = 1),
        )
        repository.save(
            ValidationTask(validationPairId = UUID.randomUUID(), edgeId = 2L, lastAttemptAt = freshTime, attempts = 1),
        )
        repository.save(
            ValidationTask(
                validationPairId = UUID.randomUUID(),
                edgeId = 3L,
                lastAttemptAt = staleTime,
                attempts = 1,
                status = OutboxStatus.SUCCESS,
            ),
        )
        em.flush()

        val stale = repository.findStale(cutoff)

        assertEquals(1, stale.size)
        assertEquals(stalePending.id, stale[0].id)
    }

    @Test
    fun `findStale — lastAttemptAt이 null인 PENDING도 포함 (한 번도 시도 안 한 row)`() {
        val nullAttemptTask = repository.save(ValidationTask(validationPairId = UUID.randomUUID(), edgeId = 100L))
        em.flush()

        val stale = repository.findStale(OffsetDateTime.now())

        assertEquals(1, stale.size)
        assertEquals(nullAttemptTask.id, stale[0].id)
    }
}
