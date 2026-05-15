package com.docgraph.backend.validation.query.infra

import com.docgraph.backend.event.OutboxStatus
import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.validation.command.domain.Conflict
import com.docgraph.backend.validation.command.domain.ConflictFinding
import com.docgraph.backend.validation.command.domain.ConflictFindingRepository
import com.docgraph.backend.validation.command.domain.ConflictRepository
import com.docgraph.backend.validation.command.domain.ValidationTask
import com.docgraph.backend.validation.command.domain.ValidationTaskRepository
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Tag("slice")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SharedPostgresContainer::class, ValidationQueryRepository::class)
class ValidationQueryRepositoryTest @Autowired constructor(
    private val queryRepository: ValidationQueryRepository,
    private val conflictRepository: ConflictRepository,
    private val findingRepository: ConflictFindingRepository,
    private val taskRepository: ValidationTaskRepository,
) {

    @Test
    fun `findConflictsByEdgeIds — 주어진 edge들의 미해소 conflict를 페이지네이션으로 반환`() {
        val now = OffsetDateTime.now()
        conflictRepository.save(Conflict(edgeId = 1L, firstDetectedAt = now, lastDetectedAt = now))
        conflictRepository.save(Conflict(edgeId = 2L, firstDetectedAt = now, lastDetectedAt = now))
        conflictRepository.save(Conflict(edgeId = 3L, firstDetectedAt = now, lastDetectedAt = now))

        val page = queryRepository.findConflictsByEdgeIds(listOf(1L, 2L), PageRequest.of(0, 10))

        assertEquals(2L, page.totalElements)
        assertTrue(page.content.all { it.edgeId in listOf(1L, 2L) })
    }

    @Test
    fun `findConflictsByEdgeIds — resolved된 conflict 제외`() {
        val now = OffsetDateTime.now()
        val active = conflictRepository.save(Conflict(edgeId = 1L, firstDetectedAt = now, lastDetectedAt = now))
        conflictRepository.save(
            Conflict(edgeId = 1L, firstDetectedAt = now, lastDetectedAt = now, resolvedAt = now),
        )

        val page = queryRepository.findConflictsByEdgeIds(listOf(1L), PageRequest.of(0, 10))

        assertEquals(1L, page.totalElements)
        assertEquals(active.id, page.content[0].id)
    }

    @Test
    fun `findConflictsByEdgeIds — 다른 edge의 conflict는 제외`() {
        val now = OffsetDateTime.now()
        conflictRepository.save(Conflict(edgeId = 99L, firstDetectedAt = now, lastDetectedAt = now))

        val page = queryRepository.findConflictsByEdgeIds(listOf(1L, 2L), PageRequest.of(0, 10))

        assertEquals(0L, page.totalElements)
    }

    @Test
    fun `findFindingsByConflictIds — conflictId별 findings grouped 반환`() {
        val now = OffsetDateTime.now()
        val conflict1 = conflictRepository.save(Conflict(edgeId = 1L, firstDetectedAt = now, lastDetectedAt = now))
        val conflict2 = conflictRepository.save(Conflict(edgeId = 2L, firstDetectedAt = now, lastDetectedAt = now))
        val task1 = taskRepository.save(ValidationTask(validationPairId = UUID.randomUUID(), edgeId = 1L))
        val task2 = taskRepository.save(ValidationTask(validationPairId = UUID.randomUUID(), edgeId = 2L))
        findingRepository.save(
            ConflictFinding(
                conflictId = conflict1.id,
                validationTaskId = task1.id,
                sourceBlockIds = listOf("b1"),
                targetBlockIds = listOf("b2"),
                rationale = "first",
                detectedAt = now,
            ),
        )
        findingRepository.save(
            ConflictFinding(
                conflictId = conflict1.id,
                validationTaskId = task1.id,
                sourceBlockIds = listOf("b3"),
                targetBlockIds = listOf("b4"),
                rationale = "second",
                detectedAt = now,
            ),
        )
        findingRepository.save(
            ConflictFinding(
                conflictId = conflict2.id,
                validationTaskId = task2.id,
                sourceBlockIds = listOf("b5"),
                targetBlockIds = listOf("b6"),
                rationale = "third",
                detectedAt = now,
            ),
        )

        val grouped = queryRepository.findFindingsByConflictIds(listOf(conflict1.id, conflict2.id))

        assertEquals(2, grouped[conflict1.id]?.size)
        assertEquals(1, grouped[conflict2.id]?.size)
    }

    @Test
    fun `findValidationTasksByEdgeIds — 주어진 edge들의 task를 페이지네이션으로 반환`() {
        taskRepository.save(ValidationTask(validationPairId = UUID.randomUUID(), edgeId = 1L))
        taskRepository.save(ValidationTask(validationPairId = UUID.randomUUID(), edgeId = 2L))
        taskRepository.save(ValidationTask(validationPairId = UUID.randomUUID(), edgeId = 99L))

        val page = queryRepository.findValidationTasksByEdgeIds(listOf(1L, 2L), PageRequest.of(0, 10))

        assertEquals(2L, page.totalElements)
        assertTrue(page.content.all { it.edgeId in listOf(1L, 2L) })
        assertTrue(page.content.all { it.status == OutboxStatus.PENDING })
    }
}