package com.docgraph.backend.validation.command.application

import com.docgraph.backend.event.OutboxStatus
import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.validation.command.domain.Conflict
import com.docgraph.backend.validation.command.domain.ConflictDetectedEvent
import com.docgraph.backend.validation.command.domain.ConflictFindingRepository
import com.docgraph.backend.validation.command.domain.ConflictRepository
import com.docgraph.backend.validation.command.domain.ConflictResolvedEvent
import com.docgraph.backend.validation.command.domain.DetectedConflict
import com.docgraph.backend.validation.command.domain.ValidationTask
import com.docgraph.backend.validation.command.domain.ValidationTaskRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.event.EventListener
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConflictDetectedProbe {
    val received: MutableList<ConflictDetectedEvent> = mutableListOf()

    @EventListener
    fun on(event: ConflictDetectedEvent) {
        received += event
    }
}

class ConflictResolvedProbe {
    val received: MutableList<ConflictResolvedEvent> = mutableListOf()

    @EventListener
    fun on(event: ConflictResolvedEvent) {
        received += event
    }
}

@TestConfiguration
class CompleteValidationTaskTestConfig {
    @Bean fun conflictDetectedProbe() = ConflictDetectedProbe()
    @Bean fun conflictResolvedProbe() = ConflictResolvedProbe()
}

@Tag("component")
@SpringBootTest
@Import(CompleteValidationTaskTestConfig::class, SharedPostgresContainer::class)
class CompleteValidationTaskCommandHandlerTest @Autowired constructor(
    private val handler: CompleteValidationTaskCommandHandler,
    private val taskRepository: ValidationTaskRepository,
    private val conflictRepository: ConflictRepository,
    private val findingRepository: ConflictFindingRepository,
    private val detectedProbe: ConflictDetectedProbe,
    private val resolvedProbe: ConflictResolvedProbe,
) {

    @BeforeEach
    fun reset() {
        findingRepository.deleteAll()
        conflictRepository.deleteAll()
        taskRepository.deleteAll()
        detectedProbe.received.clear()
        resolvedProbe.received.clear()
    }

    @Test
    fun `NoOp — 기존 활성 없음 + findings 없음, task만 SUCCESS, 이벤트 없음`() {
        val task = newPendingTask(edgeId = 31L)

        handler.handle(CompleteValidationTaskCommand(task.id, findings = emptyList()))

        val reloaded = taskRepository.findById(task.id).orElseThrow()
        assertEquals(OutboxStatus.SUCCESS, reloaded.status)
        assertEquals(0, conflictRepository.count())
        assertEquals(0, findingRepository.count())
        assertTrue(detectedProbe.received.isEmpty())
        assertTrue(resolvedProbe.received.isEmpty())
    }

    @Test
    fun `Detected — 기존 활성 없음 + findings 있음, 새 Conflict + Finding, ConflictDetectedEvent 발행`() {
        val task = newPendingTask(edgeId = 32L)
        val findings = listOf(
            DetectedConflict(sourceBlockIds = listOf("s1"), targetBlockIds = listOf("t1"), rationale = "r1", suggestion = "sug1"),
            DetectedConflict(sourceBlockIds = listOf("s2"), targetBlockIds = listOf("t2"), rationale = "r2", suggestion = "sug2"),
        )

        handler.handle(CompleteValidationTaskCommand(task.id, findings))

        assertEquals(OutboxStatus.SUCCESS, taskRepository.findById(task.id).orElseThrow().status)
        val conflicts = conflictRepository.findAll()
        assertEquals(1, conflicts.size)
        val conflict = conflicts.single()
        assertEquals(32L, conflict.edgeId)
        assertNull(conflict.resolvedAt)
        val savedFindings = findingRepository.findByConflictIdOrderByDetectedAtDesc(conflict.id)
        assertEquals(2, savedFindings.size)
        assertTrue(savedFindings.all { it.validationTaskId == task.id })
        assertEquals(setOf("sug1", "sug2"), savedFindings.map { it.suggestion }.toSet())
        assertEquals(1, detectedProbe.received.size)
        assertEquals(conflict.id, detectedProbe.received.single().conflictId)
        assertEquals(32L, detectedProbe.received.single().edgeId)
        assertTrue(resolvedProbe.received.isEmpty())
    }

    @Test
    fun `Updated — 기존 활성(ignored) + findings 있음, ignored 해제·lastDetectedAt 갱신·Finding 추가, 이벤트 없음`() {
        val task = newPendingTask(edgeId = 33L)
        val older = OffsetDateTime.now().minusMinutes(10)
        val existing = conflictRepository.saveAndFlush(
            Conflict(edgeId = 33L, firstDetectedAt = older, lastDetectedAt = older).apply {
                ignore(by = 99L, reason = "임시 무시", at = older)
            },
        )

        handler.handle(
            CompleteValidationTaskCommand(
                task.id,
                findings = listOf(DetectedConflict(listOf("s"), listOf("t"), "r", "sug")),
            ),
        )

        val reloaded = conflictRepository.findById(existing.id).orElseThrow()
        assertNull(reloaded.resolvedAt)
        assertNull(reloaded.ignoredAt)
        assertNull(reloaded.ignoredBy)
        assertNull(reloaded.ignoreReason)
        assertTrue(reloaded.lastDetectedAt.isAfter(older), "lastDetectedAt 갱신되어야 함")
        val findings = findingRepository.findByConflictIdOrderByDetectedAtDesc(existing.id)
        assertEquals(1, findings.size)
        assertEquals(task.id, findings.single().validationTaskId)
        assertTrue(detectedProbe.received.isEmpty())
        assertTrue(resolvedProbe.received.isEmpty())
    }

    @Test
    fun `Resolved — 기존 활성 + findings 없음, resolvedAt 세팅·ConflictResolvedEvent 발행`() {
        val task = newPendingTask(edgeId = 34L)
        val older = OffsetDateTime.now().minusMinutes(10)
        val existing = conflictRepository.saveAndFlush(
            Conflict(edgeId = 34L, firstDetectedAt = older, lastDetectedAt = older),
        )

        handler.handle(CompleteValidationTaskCommand(task.id, findings = emptyList()))

        val reloaded = conflictRepository.findById(existing.id).orElseThrow()
        assertNotNull(reloaded.resolvedAt)
        assertEquals(0, findingRepository.count())
        assertTrue(detectedProbe.received.isEmpty())
        assertEquals(1, resolvedProbe.received.size)
        assertEquals(existing.id, resolvedProbe.received.single().conflictId)
        assertEquals(34L, resolvedProbe.received.single().edgeId)
    }

    private fun newPendingTask(edgeId: Long): ValidationTask =
        taskRepository.saveAndFlush(ValidationTask(validationPairId = UUID.randomUUID(), edgeId = edgeId))
}
