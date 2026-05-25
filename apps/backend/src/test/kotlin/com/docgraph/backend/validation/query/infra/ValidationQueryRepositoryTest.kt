package com.docgraph.backend.validation.query.infra

import com.docgraph.backend.event.OutboxStatus
import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.validation.command.domain.Conflict
import com.docgraph.backend.validation.command.domain.ConflictFinding
import com.docgraph.backend.validation.command.domain.ConflictFindingRepository
import com.docgraph.backend.validation.command.domain.ConflictRepository
import com.docgraph.backend.validation.command.domain.ValidationTask
import com.docgraph.backend.validation.command.domain.ValidationTaskRepository
import com.docgraph.backend.validation.command.infra.ConflictFindingRepositoryImpl
import com.docgraph.backend.validation.command.infra.ConflictRepositoryImpl
import com.docgraph.backend.validation.command.infra.ValidationTaskRepositoryImpl
import com.docgraph.backend.validation.query.application.MyConflictStatusFilter
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
@Import(
    SharedPostgresContainer::class,
    ValidationQueryRepository::class,
    ConflictRepositoryImpl::class,
    ConflictFindingRepositoryImpl::class,
    ValidationTaskRepositoryImpl::class,
)
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
                targetBlockId = "b2",
                rationale = "first",
                newText = "sug1",
                title = "tt1",
                detectedAt = now,
            ),
        )
        findingRepository.save(
            ConflictFinding(
                conflictId = conflict1.id,
                validationTaskId = task1.id,
                sourceBlockIds = listOf("b3"),
                targetBlockId = "b4",
                rationale = "second",
                newText = "sug2",
                title = "tt2",
                detectedAt = now,
            ),
        )
        findingRepository.save(
            ConflictFinding(
                conflictId = conflict2.id,
                validationTaskId = task2.id,
                sourceBlockIds = listOf("b5"),
                targetBlockId = "b6",
                rationale = "third",
                newText = "sug3",
                title = "tt3",
                detectedAt = now,
            ),
        )

        val grouped = queryRepository.findFindingsByConflictIds(listOf(conflict1.id, conflict2.id))
            .groupBy { it.conflictId }

        assertEquals(2, grouped[conflict1.id]?.size)
        assertEquals(1, grouped[conflict2.id]?.size)
        assertEquals("tt3", grouped[conflict2.id]?.single()?.title)
    }

    @Test
    fun `findInboxConflictsByEdgeIds — ACTIVE filter는 무시되지 않은 미해소 conflict만`() {
        val now = OffsetDateTime.now()
        val active = conflictRepository.save(Conflict(edgeId = 1L, firstDetectedAt = now, lastDetectedAt = now))
        conflictRepository.save(
            Conflict(edgeId = 2L, firstDetectedAt = now, lastDetectedAt = now, ignoredAt = now, ignoredBy = 1L),
        )

        val page = queryRepository.findInboxConflictsByEdgeIds(
            listOf(1L, 2L), MyConflictStatusFilter.ACTIVE, PageRequest.of(0, 10),
        )

        assertEquals(1L, page.totalElements)
        assertEquals(active.id, page.content[0].id)
    }

    @Test
    fun `findInboxConflictsByEdgeIds — IGNORED filter는 무시된 미해소 conflict만`() {
        val now = OffsetDateTime.now()
        conflictRepository.save(Conflict(edgeId = 1L, firstDetectedAt = now, lastDetectedAt = now))
        val ignored = conflictRepository.save(
            Conflict(edgeId = 2L, firstDetectedAt = now, lastDetectedAt = now, ignoredAt = now, ignoredBy = 1L),
        )

        val page = queryRepository.findInboxConflictsByEdgeIds(
            listOf(1L, 2L), MyConflictStatusFilter.IGNORED, PageRequest.of(0, 10),
        )

        assertEquals(1L, page.totalElements)
        assertEquals(ignored.id, page.content[0].id)
    }

    @Test
    fun `findInboxConflictsByEdgeIds — ALL filter는 active와 ignored 모두`() {
        val now = OffsetDateTime.now()
        conflictRepository.save(Conflict(edgeId = 1L, firstDetectedAt = now, lastDetectedAt = now))
        conflictRepository.save(
            Conflict(edgeId = 2L, firstDetectedAt = now, lastDetectedAt = now, ignoredAt = now, ignoredBy = 1L),
        )

        val page = queryRepository.findInboxConflictsByEdgeIds(
            listOf(1L, 2L), MyConflictStatusFilter.ALL, PageRequest.of(0, 10),
        )

        assertEquals(2L, page.totalElements)
    }

    @Test
    fun `findInboxConflictsByEdgeIds — resolved는 모든 filter에서 제외`() {
        val now = OffsetDateTime.now()
        conflictRepository.save(
            Conflict(edgeId = 1L, firstDetectedAt = now, lastDetectedAt = now, resolvedAt = now),
        )

        listOf(MyConflictStatusFilter.ACTIVE, MyConflictStatusFilter.IGNORED, MyConflictStatusFilter.ALL).forEach { f ->
            val page = queryRepository.findInboxConflictsByEdgeIds(listOf(1L), f, PageRequest.of(0, 10))
            assertEquals(0L, page.totalElements, "filter=$f should exclude resolved")
        }
    }

    @Test
    fun `findInboxConflictsByEdgeIds — firstDetectedAt와 ignoredAt 매핑`() {
        val detectedAt = OffsetDateTime.parse("2026-05-01T00:00:00Z")
        val ignoredAt = OffsetDateTime.parse("2026-05-10T00:00:00Z")
        conflictRepository.save(
            Conflict(
                edgeId = 1L,
                firstDetectedAt = detectedAt,
                lastDetectedAt = detectedAt,
                ignoredAt = ignoredAt,
                ignoredBy = 1L,
            ),
        )

        val page = queryRepository.findInboxConflictsByEdgeIds(
            listOf(1L), MyConflictStatusFilter.IGNORED, PageRequest.of(0, 10),
        )

        val row = page.content.single()
        assertEquals(detectedAt, row.firstDetectedAt)
        assertEquals(ignoredAt, row.ignoredAt)
    }

    @Test
    fun `findInboxConflictsByEdgeIds — 페이지네이션 적용`() {
        val now = OffsetDateTime.now()
        listOf(1L, 2L, 3L).forEach { edgeId ->
            conflictRepository.save(Conflict(edgeId = edgeId, firstDetectedAt = now, lastDetectedAt = now))
        }

        val page0 = queryRepository.findInboxConflictsByEdgeIds(
            listOf(1L, 2L, 3L), MyConflictStatusFilter.ACTIVE, PageRequest.of(0, 2),
        )
        val page1 = queryRepository.findInboxConflictsByEdgeIds(
            listOf(1L, 2L, 3L), MyConflictStatusFilter.ACTIVE, PageRequest.of(1, 2),
        )

        assertEquals(3L, page0.totalElements)
        assertEquals(2, page0.totalPages)
        assertEquals(2, page0.content.size)
        assertEquals(1, page1.content.size)
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

    @Test
    fun `findActiveUnignoredEdgeIds — 활성 미무시 conflict가 있는 edgeId 반환`() {
        val now = OffsetDateTime.now()
        conflictRepository.save(Conflict(edgeId = 1L, firstDetectedAt = now, lastDetectedAt = now))
        conflictRepository.save(
            Conflict(edgeId = 2L, firstDetectedAt = now, lastDetectedAt = now, resolvedAt = now),
        )
        conflictRepository.save(
            Conflict(edgeId = 3L, firstDetectedAt = now, lastDetectedAt = now, ignoredAt = now, ignoredBy = 99L),
        )
        conflictRepository.save(Conflict(edgeId = 4L, firstDetectedAt = now, lastDetectedAt = now))

        val ids = queryRepository.findActiveUnignoredEdgeIds(listOf(1L, 2L, 3L, 4L)).toSet()

        assertEquals(setOf(1L, 4L), ids)
    }

    @Test
    fun `findActiveUnignoredEdgeIds — 빈 입력이면 빈 리스트`() {
        val ids = queryRepository.findActiveUnignoredEdgeIds(emptyList())
        assertTrue(ids.isEmpty())
    }

    @Test
    fun `findActiveUnignoredEdgeIds — 입력에 포함되지 않은 edge는 제외`() {
        val now = OffsetDateTime.now()
        conflictRepository.save(Conflict(edgeId = 99L, firstDetectedAt = now, lastDetectedAt = now))

        val ids = queryRepository.findActiveUnignoredEdgeIds(listOf(1L, 2L))

        assertTrue(ids.isEmpty())
    }
}