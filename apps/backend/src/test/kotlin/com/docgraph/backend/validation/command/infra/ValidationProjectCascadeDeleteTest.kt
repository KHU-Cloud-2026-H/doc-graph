package com.docgraph.backend.validation.command.infra

import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.validation.command.domain.AiUsageRecord
import com.docgraph.backend.validation.command.domain.AiUsageRecordRepository
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Tag("slice")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(
    SharedPostgresContainer::class,
    ValidationTaskRepositoryImpl::class,
    ConflictRepositoryImpl::class,
    ConflictFindingRepositoryImpl::class,
    AiUsageRecordRepositoryImpl::class,
)
class ValidationProjectCascadeDeleteTest @Autowired constructor(
    private val taskRepository: ValidationTaskRepository,
    private val conflictRepository: ConflictRepository,
    private val findingRepository: ConflictFindingRepository,
    private val aiUsageRecordRepository: AiUsageRecordRepository,
) {

    @PersistenceContext
    private lateinit var em: EntityManager

    @Test
    fun `task deleteByProjectId — 해당 프로젝트 task 삭제 + conflict_finding(task FK) cascade, 타 프로젝트 보존`() {
        val now = OffsetDateTime.now()
        val task = taskRepository.save(ValidationTask(validationPairId = UUID.randomUUID(), edgeId = 100L, projectId = 1L))
        val other = taskRepository.save(ValidationTask(validationPairId = UUID.randomUUID(), edgeId = 200L, projectId = 2L))
        val conflict = conflictRepository.save(Conflict(edgeId = 100L, projectId = 1L, firstDetectedAt = now, lastDetectedAt = now))
        val finding = findingRepository.save(newFinding(conflict.id, task.id, now))
        em.flush()

        taskRepository.deleteByProjectId(1L)
        em.flush()
        em.clear()

        assertNull(taskRepository.findById(task.id))
        assertNotNull(taskRepository.findById(other.id))
        assertNull(findingRepository.findById(finding.id)) // validation_task_id FK ON DELETE CASCADE
    }

    @Test
    fun `conflict deleteByProjectId — 해당 프로젝트 conflict 삭제 + conflict_finding(conflict FK) cascade, 타 프로젝트 보존`() {
        val now = OffsetDateTime.now()
        val task = taskRepository.save(ValidationTask(validationPairId = UUID.randomUUID(), edgeId = 101L, projectId = 1L))
        val conflict = conflictRepository.save(Conflict(edgeId = 101L, projectId = 1L, firstDetectedAt = now, lastDetectedAt = now))
        val other = conflictRepository.save(Conflict(edgeId = 201L, projectId = 2L, firstDetectedAt = now, lastDetectedAt = now))
        val finding = findingRepository.save(newFinding(conflict.id, task.id, now))
        em.flush()

        conflictRepository.deleteByProjectId(1L)
        em.flush()
        em.clear()

        assertNull(conflictRepository.findById(conflict.id))
        assertNotNull(conflictRepository.findById(other.id))
        assertNull(findingRepository.findById(finding.id)) // conflict_id FK ON DELETE CASCADE
    }

    @Test
    fun `ai_usage_record deleteByProjectId — 해당 프로젝트만 삭제, 타 프로젝트 보존`() {
        aiUsageRecordRepository.save(aiUsage(validationTaskId = 501L, projectId = 1L))
        aiUsageRecordRepository.save(aiUsage(validationTaskId = 502L, projectId = 2L))
        em.flush()

        aiUsageRecordRepository.deleteByProjectId(1L)
        em.flush()
        em.clear()

        assertNull(aiUsageRecordRepository.findByValidationTaskId(501L))
        assertNotNull(aiUsageRecordRepository.findByValidationTaskId(502L))
    }

    private fun newFinding(conflictId: Long, taskId: Long, detectedAt: OffsetDateTime) =
        ConflictFinding(
            conflictId = conflictId,
            validationTaskId = taskId,
            sourceBlockIds = listOf("s"),
            targetBlockId = "t",
            rationale = "r",
            newText = "sug",
            title = "tt",
            detectedAt = detectedAt,
        )

    private fun aiUsage(validationTaskId: Long, projectId: Long) =
        AiUsageRecord(
            validationTaskId = validationTaskId,
            projectId = projectId,
            model = "gpt-x",
            promptTokens = 1,
            completionTokens = 1,
            totalTokens = 2,
        )
}
