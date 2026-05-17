package com.docgraph.backend.validation.command.domain

import com.docgraph.backend.fixtures.SharedPostgresContainer
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

@Tag("slice")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SharedPostgresContainer::class)
class ConflictFindingRepositoryTest @Autowired constructor(
    private val findingRepository: ConflictFindingRepository,
    private val conflictRepository: ConflictRepository,
    private val taskRepository: ValidationTaskRepository,
    private val em: EntityManager,
) {

    @Test
    fun `JSONB List String 라운드트립`() {
        val task = taskRepository.saveAndFlush(
            ValidationTask(validationPairId = UUID.randomUUID(), edgeId = 21L),
        )
        val now = OffsetDateTime.now()
        val conflict = conflictRepository.saveAndFlush(
            Conflict(edgeId = 21L, firstDetectedAt = now, lastDetectedAt = now),
        )

        val saved = findingRepository.saveAndFlush(
            ConflictFinding(
                conflictId = conflict.id,
                validationTaskId = task.id,
                sourceBlockIds = listOf("block-1", "block-2"),
                targetBlockIds = listOf("target-1"),
                rationale = "회의록의 결정사항이 요구사항에 반영되지 않음",
                suggestion = "requirements 3.2절을 'A 옵션'으로 변경",
                detectedAt = now,
            ),
        )
        em.clear()

        val loaded = findingRepository.findById(saved.id).orElseThrow()
        assertEquals(listOf("block-1", "block-2"), loaded.sourceBlockIds)
        assertEquals(listOf("target-1"), loaded.targetBlockIds)
        assertEquals("회의록의 결정사항이 요구사항에 반영되지 않음", loaded.rationale)
        assertEquals("requirements 3.2절을 'A 옵션'으로 변경", loaded.suggestion)
    }

    @Test
    fun `approvedAt·approvedBy 라운드트립 — 초기 null, approve 후 영속`() {
        val task = taskRepository.saveAndFlush(
            ValidationTask(validationPairId = UUID.randomUUID(), edgeId = 23L),
        )
        val now = OffsetDateTime.now()
        val conflict = conflictRepository.saveAndFlush(
            Conflict(edgeId = 23L, firstDetectedAt = now, lastDetectedAt = now),
        )

        val initial = findingRepository.saveAndFlush(newFinding(conflict.id, task.id, now))
        em.clear()

        val loadedInitial = findingRepository.findById(initial.id).orElseThrow()
        assertNull(loadedInitial.approvedAt)
        assertNull(loadedInitial.approvedBy)

        val approveAt = OffsetDateTime.parse("2026-05-17T10:00:00Z")
        loadedInitial.approve(by = 77L, at = approveAt)
        findingRepository.saveAndFlush(loadedInitial)
        em.clear()

        val loadedApproved = findingRepository.findById(initial.id).orElseThrow()
        assertEquals(approveAt, loadedApproved.approvedAt)
        assertEquals(77L, loadedApproved.approvedBy)
    }

    @Test
    fun `findByConflictIdOrderByDetectedAtDesc — 최신순 반환`() {
        val task = taskRepository.saveAndFlush(
            ValidationTask(validationPairId = UUID.randomUUID(), edgeId = 22L),
        )
        val now = OffsetDateTime.now()
        val conflict = conflictRepository.saveAndFlush(
            Conflict(edgeId = 22L, firstDetectedAt = now, lastDetectedAt = now),
        )

        val older = findingRepository.saveAndFlush(
            newFinding(conflictId = conflict.id, taskId = task.id, detectedAt = now.minusMinutes(5)),
        )
        val newer = findingRepository.saveAndFlush(
            newFinding(conflictId = conflict.id, taskId = task.id, detectedAt = now),
        )

        val list = findingRepository.findByConflictIdOrderByDetectedAtDesc(conflict.id)

        assertEquals(listOf(newer.id, older.id), list.map { it.id })
    }

    private fun newFinding(conflictId: Long, taskId: Long, detectedAt: OffsetDateTime) =
        ConflictFinding(
            conflictId = conflictId,
            validationTaskId = taskId,
            sourceBlockIds = listOf("s"),
            targetBlockIds = listOf("t"),
            rationale = "r",
            suggestion = "sug",
            detectedAt = detectedAt,
        )
}
