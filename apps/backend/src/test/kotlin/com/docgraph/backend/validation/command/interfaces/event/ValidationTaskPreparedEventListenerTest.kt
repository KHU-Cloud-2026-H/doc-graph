package com.docgraph.backend.validation.command.interfaces.event

import com.docgraph.backend.document.query.application.Block
import com.docgraph.backend.document.query.application.DocumentDetail
import com.docgraph.backend.document.query.application.DocumentType
import com.docgraph.backend.document.query.application.FindDocumentByIdQuery
import com.docgraph.backend.event.OutboxStatus
import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.graph.query.application.EdgeDetail
import com.docgraph.backend.graph.query.application.FindEdgeByIdQuery
import com.docgraph.backend.validation.command.domain.ConflictDetector
import com.docgraph.backend.validation.command.domain.ConflictFindingRepository
import com.docgraph.backend.validation.command.domain.ConflictRepository
import com.docgraph.backend.validation.command.domain.DetectedConflict
import com.docgraph.backend.validation.command.domain.ValidationTask
import com.docgraph.backend.validation.command.domain.ValidationTaskPreparedEvent
import com.docgraph.backend.validation.command.domain.ValidationTaskRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ListenerFakeFindEdgeByIdQuery : FindEdgeByIdQuery {
    @Volatile var behavior: (Long) -> EdgeDetail? = { null }
    override fun find(edgeId: Long): EdgeDetail? = behavior(edgeId)
}

class ListenerFakeFindDocumentByIdQuery : FindDocumentByIdQuery {
    @Volatile var behavior: (Long) -> DocumentDetail? = { null }
    override fun find(documentId: Long): DocumentDetail? = behavior(documentId)
}

class FakeConflictDetector : ConflictDetector {
    @Volatile var behavior: (List<Block>, List<Block>, String) -> List<DetectedConflict> =
        { _, _, _ -> emptyList() }
    val invocations = AtomicInteger(0)

    override fun detect(
        changedBlocks: List<Block>,
        counterpartBlocks: List<Block>,
        criterion: String,
    ): List<DetectedConflict> {
        invocations.incrementAndGet()
        return behavior(changedBlocks, counterpartBlocks, criterion)
    }
}

open class ListenerTestPublisher(private val publisher: ApplicationEventPublisher) {
    @Transactional
    open fun publish(event: ValidationTaskPreparedEvent) {
        publisher.publishEvent(event)
    }
}

@TestConfiguration
class ListenerTestConfig {
    @Bean @Primary fun listenerFakeFindEdge() = ListenerFakeFindEdgeByIdQuery()
    @Bean @Primary fun listenerFakeFindDocument() = ListenerFakeFindDocumentByIdQuery()
    @Bean @Primary fun listenerFakeDetector() = FakeConflictDetector()
    @Bean fun listenerTestPublisher(p: ApplicationEventPublisher) = ListenerTestPublisher(p)
}

@Tag("component")
@SpringBootTest
@Import(ListenerTestConfig::class, SharedPostgresContainer::class)
@TestPropertySource(
    properties = [
        "validation.task.detect.max-attempts=2",
        "validation.task.detect.retry-delay-ms=10",
        "validation.task.detect.retry-multiplier=1.0",
        "validation.task.detect.retry-max-delay-ms=20",
    ],
)
class ValidationTaskPreparedEventListenerTest @Autowired constructor(
    private val testPublisher: ListenerTestPublisher,
    private val taskRepository: ValidationTaskRepository,
    private val conflictRepository: ConflictRepository,
    private val findingRepository: ConflictFindingRepository,
    private val em: EntityManager,
    private val txTemplate: TransactionTemplate,
    private val findEdge: ListenerFakeFindEdgeByIdQuery,
    private val findDocument: ListenerFakeFindDocumentByIdQuery,
    private val detector: FakeConflictDetector,
) {

    @BeforeEach
    fun reset() {
        txTemplate.executeWithoutResult {
            em.createQuery("DELETE FROM ConflictFinding").executeUpdate()
            em.createQuery("DELETE FROM Conflict").executeUpdate()
            em.createQuery("DELETE FROM ValidationTask").executeUpdate()
        }
        detector.invocations.set(0)
        detector.behavior = { _, _, _ -> emptyList() }
    }

    @Test
    fun `happy — AI 결과로 CompleteHandler가 task를 SUCCESS로 전환하고 Conflict 영속`() {
        val task = newPendingTask(edgeId = 41L)
        wireFakes(
            edgeId = 41L,
            sourceBlocks = listOf(Block("s1", null, "paragraph", "source text", 0)),
            targetBlocks = listOf(Block("t1", null, "paragraph", "target text", 0)),
        )
        detector.behavior = { _, _, _ ->
            listOf(DetectedConflict(listOf("s1"), listOf("t1"), "불일치", "수정 제안"))
        }

        testPublisher.publish(ValidationTaskPreparedEvent(task.id, OffsetDateTime.now()))

        waitFor { taskRepository.findById(task.id)?.status == OutboxStatus.SUCCESS }
        assertEquals(1, detector.invocations.get())
        assertEquals(1L, conflictCount())
        assertEquals(1L, findingCount())
    }

    @Test
    fun `AI 호출 실패 — Retryable 모두 소진 후 Recover로 task FAILED`() {
        val task = newPendingTask(edgeId = 42L)
        wireFakes(
            edgeId = 42L,
            sourceBlocks = listOf(Block("s", null, "paragraph", "s", 0)),
            targetBlocks = listOf(Block("t", null, "paragraph", "t", 0)),
        )
        detector.behavior = { _, _, _ -> throw RuntimeException("AI failure") }

        testPublisher.publish(ValidationTaskPreparedEvent(task.id, OffsetDateTime.now()))

        waitFor { taskRepository.findById(task.id)?.status == OutboxStatus.FAILED }
        val reloaded = taskRepository.findById(task.id)!!
        assertEquals("AI failure", reloaded.failureReason)
        assertEquals(2, detector.invocations.get(), "max-attempts=2 만큼 호출")
        assertEquals(0L, conflictCount())
    }

    private fun newPendingTask(edgeId: Long): ValidationTask =
        taskRepository.save(ValidationTask(validationPairId = UUID.randomUUID(), edgeId = edgeId))

    private fun conflictCount(): Long =
        em.createQuery("SELECT COUNT(c) FROM Conflict c", Long::class.javaObjectType).singleResult

    private fun findingCount(): Long =
        em.createQuery("SELECT COUNT(f) FROM ConflictFinding f", Long::class.javaObjectType).singleResult

    private fun wireFakes(edgeId: Long, sourceBlocks: List<Block>, targetBlocks: List<Block>) {
        val sourceDocId = 1000L + edgeId
        val targetDocId = 2000L + edgeId
        findEdge.behavior = { id ->
            if (id == edgeId) EdgeDetail(edgeId, sourceDocId, targetDocId, "criterion") else null
        }
        findDocument.behavior = { id ->
            when (id) {
                sourceDocId -> DocumentDetail(id, "p-$id", "src", DocumentType.MEETING_NOTES, null, null, sourceBlocks)
                targetDocId -> DocumentDetail(id, "p-$id", "tgt", DocumentType.REQUIREMENTS, null, null, targetBlocks)
                else -> null
            }
        }
    }

    private fun waitFor(
        timeoutMs: Long = 10_000,
        intervalMs: Long = 50,
        predicate: () -> Boolean,
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (predicate()) return
            Thread.sleep(intervalMs)
        }
        assertTrue(false, "timeout after ${timeoutMs}ms")
    }
}
