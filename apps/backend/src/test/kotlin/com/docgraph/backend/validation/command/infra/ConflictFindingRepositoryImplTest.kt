package com.docgraph.backend.validation.command.infra

import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.validation.command.domain.Conflict
import com.docgraph.backend.validation.command.domain.ConflictFinding
import com.docgraph.backend.validation.command.domain.ConflictFindingRepository
import com.docgraph.backend.validation.command.domain.ConflictRepository
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
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Tag("slice")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(
    SharedPostgresContainer::class,
    ConflictRepositoryImpl::class,
    ConflictFindingRepositoryImpl::class,
    ValidationTaskRepositoryImpl::class,
)
class ConflictFindingRepositoryImplTest @Autowired constructor(
    private val findingRepository: ConflictFindingRepository,
    private val conflictRepository: ConflictRepository,
    private val taskRepository: ValidationTaskRepository,
) {

    @PersistenceContext
    private lateinit var em: EntityManager

    @Test
    fun `ConflictFinding 라운드트립 — source_block_ids JSONB + target_block_id·new_text TEXT`() {
        val task = taskRepository.save(ValidationTask(validationPairId = UUID.randomUUID(), edgeId = 21L))
        em.flush()
        val now = OffsetDateTime.now()
        val conflict = conflictRepository.save(
            Conflict(edgeId = 21L, firstDetectedAt = now, lastDetectedAt = now),
        )
        em.flush()

        val saved = findingRepository.save(
            ConflictFinding(
                conflictId = conflict.id,
                validationTaskId = task.id,
                sourceBlockIds = listOf("block-1", "block-2"),
                targetBlockId = "target-1",
                rationale = "회의록의 결정사항이 요구사항에 반영되지 않음",
                newText = "requirements 3.2절을 'A 옵션'으로 변경",
                detectedAt = now,
            ),
        )
        em.flush()
        em.clear()

        val loaded = findingRepository.findById(saved.id)
        assertNotNull(loaded)
        assertEquals(listOf("block-1", "block-2"), loaded.sourceBlockIds)
        assertEquals("target-1", loaded.targetBlockId)
        assertEquals("회의록의 결정사항이 요구사항에 반영되지 않음", loaded.rationale)
        assertEquals("requirements 3.2절을 'A 옵션'으로 변경", loaded.newText)
    }

    @Test
    fun `approvedAt·approvedBy 라운드트립 — 초기 null, approve 후 영속`() {
        val task = taskRepository.save(ValidationTask(validationPairId = UUID.randomUUID(), edgeId = 23L))
        em.flush()
        val now = OffsetDateTime.now()
        val conflict = conflictRepository.save(
            Conflict(edgeId = 23L, firstDetectedAt = now, lastDetectedAt = now),
        )
        em.flush()

        val initial = findingRepository.save(newFinding(conflict.id, task.id, now))
        em.flush()
        em.clear()

        val loadedInitial = findingRepository.findById(initial.id)
        assertNotNull(loadedInitial)
        assertNull(loadedInitial.approvedAt)
        assertNull(loadedInitial.approvedBy)

        val approveAt = OffsetDateTime.parse("2026-05-17T10:00:00Z")
        loadedInitial.approve(by = 77L, at = approveAt)
        findingRepository.save(loadedInitial)
        em.flush()
        em.clear()

        val loadedApproved = findingRepository.findById(initial.id)
        assertNotNull(loadedApproved)
        assertEquals(approveAt, loadedApproved.approvedAt)
        assertEquals(77L, loadedApproved.approvedBy)
    }

    @Test
    fun `findByConflictIdOrderByDetectedAtDesc — 최신순 반환`() {
        val task = taskRepository.save(ValidationTask(validationPairId = UUID.randomUUID(), edgeId = 22L))
        em.flush()
        val now = OffsetDateTime.now()
        val conflict = conflictRepository.save(
            Conflict(edgeId = 22L, firstDetectedAt = now, lastDetectedAt = now),
        )
        em.flush()

        val older = findingRepository.save(
            newFinding(conflictId = conflict.id, taskId = task.id, detectedAt = now.minusMinutes(5)),
        )
        val newer = findingRepository.save(
            newFinding(conflictId = conflict.id, taskId = task.id, detectedAt = now),
        )
        em.flush()

        val list = findingRepository.findByConflictIdOrderByDetectedAtDesc(conflict.id)

        assertEquals(listOf(newer.id, older.id), list.map { it.id })
    }

    private fun newFinding(conflictId: Long, taskId: Long, detectedAt: OffsetDateTime) =
        ConflictFinding(
            conflictId = conflictId,
            validationTaskId = taskId,
            sourceBlockIds = listOf("s"),
            targetBlockId = "t",
            rationale = "r",
            newText = "sug",
            detectedAt = detectedAt,
        )
}
