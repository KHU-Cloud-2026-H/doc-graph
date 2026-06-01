package com.docgraph.backend.validation.command.interfaces.event

import com.docgraph.backend.document.query.application.Block
import com.docgraph.backend.document.query.application.DocumentDetail
import com.docgraph.backend.document.query.application.DocumentType
import com.docgraph.backend.document.query.application.FindDocumentByIdQuery
import com.docgraph.backend.event.OutboxStatus
import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.graph.query.application.EdgeDetail
import com.docgraph.backend.graph.query.application.FindEdgeByIdQuery
import com.docgraph.backend.validation.command.domain.ConflictDetectionInput
import com.docgraph.backend.validation.command.domain.ConflictDetectionResponseException
import com.docgraph.backend.validation.command.domain.ConflictDetector
import com.docgraph.backend.validation.command.domain.FailureCategory
import com.docgraph.backend.validation.command.domain.FirstValidationInput
import com.docgraph.backend.validation.command.domain.RevalidationInput
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
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
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
    @Volatile var behavior: (ConflictDetectionInput) -> List<DetectedConflict> = { emptyList() }
    val invocations = AtomicInteger(0)

    override fun detect(input: ConflictDetectionInput): List<DetectedConflict> {
        invocations.incrementAndGet()
        return behavior(input)
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
        detector.behavior = { _ -> emptyList() }
    }

    @Test
    fun `happy — AI 결과로 CompleteHandler가 task를 SUCCESS로 전환하고 Conflict 영속`() {
        val task = newPendingTask(edgeId = 41L)
        wireFakes(
            edgeId = 41L,
            sourceBlocks = listOf(Block("s1", null, "paragraph", "source text", null, 0)),
            targetBlocks = listOf(Block("t1", null, "paragraph", "target text", null, 0)),
        )
        detector.behavior = { _ ->
            listOf(DetectedConflict(listOf("s1"), "t1", "불일치", "수정 제안", "제목"))
        }

        testPublisher.publish(ValidationTaskPreparedEvent(task.id, OffsetDateTime.now()))

        waitFor { taskRepository.findById(task.id)?.status == OutboxStatus.SUCCESS }
        assertEquals(1, detector.invocations.get())
        assertEquals(1L, conflictCount())
        assertEquals(1L, findingCount())
    }

    @Test
    fun `transient 실패(5xx) — Retryable 모두 소진 후 Recover로 task FAILED`() {
        val task = newPendingTask(edgeId = 42L)
        wireFakes(
            edgeId = 42L,
            sourceBlocks = listOf(Block("s", null, "paragraph", "s", null, 0)),
            targetBlocks = listOf(Block("t", null, "paragraph", "t", null, 0)),
        )
        detector.behavior = { _ -> throw HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR) }

        testPublisher.publish(ValidationTaskPreparedEvent(task.id, OffsetDateTime.now()))

        waitFor { taskRepository.findById(task.id)?.status == OutboxStatus.FAILED }
        assertEquals(2, detector.invocations.get(), "max-attempts=2 만큼 호출")
        assertEquals(FailureCategory.UPSTREAM_ERROR, taskRepository.findById(task.id)?.failureCategory)
        assertEquals(0L, conflictCount())
    }

    @Test
    fun `429(rate limit) — transient으로 max-attempts 만큼 재시도`() {
        val task = newPendingTask(edgeId = 45L)
        wireFakes(
            edgeId = 45L,
            sourceBlocks = listOf(Block("s", null, "paragraph", "s", null, 0)),
            targetBlocks = listOf(Block("t", null, "paragraph", "t", null, 0)),
        )
        detector.behavior = { _ ->
            throw HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", HttpHeaders.EMPTY, ByteArray(0), null,
            )
        }

        testPublisher.publish(ValidationTaskPreparedEvent(task.id, OffsetDateTime.now()))

        waitFor { taskRepository.findById(task.id)?.status == OutboxStatus.FAILED }
        assertEquals(2, detector.invocations.get(), "429는 재시도 대상이므로 max-attempts 만큼 호출")
        assertEquals(FailureCategory.RATE_LIMITED, taskRepository.findById(task.id)?.failureCategory)
    }

    @Test
    fun `비-429 4xx(400) — 재시도 없이 1회 후 즉시 FAILED`() {
        val task = newPendingTask(edgeId = 46L)
        wireFakes(
            edgeId = 46L,
            sourceBlocks = listOf(Block("s", null, "paragraph", "s", null, 0)),
            targetBlocks = listOf(Block("t", null, "paragraph", "t", null, 0)),
        )
        detector.behavior = { _ -> throw HttpClientErrorException(HttpStatus.BAD_REQUEST) }

        testPublisher.publish(ValidationTaskPreparedEvent(task.id, OffsetDateTime.now()))

        waitFor { taskRepository.findById(task.id)?.status == OutboxStatus.FAILED }
        assertEquals(1, detector.invocations.get(), "비-429 4xx는 재시도하지 않으므로 1회만 호출")
        assertEquals(FailureCategory.INVALID_REQUEST, taskRepository.findById(task.id)?.failureCategory)
        assertEquals(0L, conflictCount())
    }

    @Test
    fun `응답 스키마 불일치 — 재시도 없이 INVALID_RESPONSE로 FAILED`() {
        val task = newPendingTask(edgeId = 47L)
        wireFakes(
            edgeId = 47L,
            sourceBlocks = listOf(Block("s", null, "paragraph", "s", null, 0)),
            targetBlocks = listOf(Block("t", null, "paragraph", "t", null, 0)),
        )
        detector.behavior = { _ -> throw ConflictDetectionResponseException("스키마 불일치") }

        testPublisher.publish(ValidationTaskPreparedEvent(task.id, OffsetDateTime.now()))

        waitFor { taskRepository.findById(task.id)?.status == OutboxStatus.FAILED }
        assertEquals(1, detector.invocations.get(), "응답 오류는 재시도하지 않으므로 1회만 호출")
        assertEquals(FailureCategory.INVALID_RESPONSE, taskRepository.findById(task.id)?.failureCategory)
        assertEquals(0L, conflictCount())
    }

    @Test
    fun `source 블록에 previousText 없음 → FirstValidationInput으로 AI 호출`() {
        val task = newPendingTask(edgeId = 43L)
        wireFakes(
            edgeId = 43L,
            sourceBlocks = listOf(Block("s1", null, "paragraph", "source text", null, 0)),
            targetBlocks = listOf(Block("t1", null, "paragraph", "target text", null, 0)),
        )
        var capturedInput: ConflictDetectionInput? = null
        detector.behavior = { input -> capturedInput = input; emptyList() }

        testPublisher.publish(ValidationTaskPreparedEvent(task.id, OffsetDateTime.now()))

        waitFor { taskRepository.findById(task.id)?.status == OutboxStatus.SUCCESS }
        assertTrue(capturedInput is FirstValidationInput, "previousText 없으면 FirstValidationInput")
    }

    @Test
    fun `source 블록에 previousText 있음 → RevalidationInput으로 AI 호출`() {
        val task = newPendingTask(edgeId = 44L)
        wireFakes(
            edgeId = 44L,
            sourceBlocks = listOf(
                Block("s1", null, "paragraph", "변경된 텍스트", "이전 텍스트", 0),
            ),
            targetBlocks = listOf(Block("t1", null, "paragraph", "target text", null, 0)),
        )
        var capturedInput: ConflictDetectionInput? = null
        detector.behavior = { input -> capturedInput = input; emptyList() }

        testPublisher.publish(ValidationTaskPreparedEvent(task.id, OffsetDateTime.now()))

        waitFor { taskRepository.findById(task.id)?.status == OutboxStatus.SUCCESS }
        assertTrue(capturedInput is RevalidationInput, "previousText 있으면 RevalidationInput")
        val reval = capturedInput as RevalidationInput
        assertEquals("이전 텍스트", reval.sourceBeforeBlocks.first().text, "before = previousText")
        assertEquals("변경된 텍스트", reval.sourceAfterBlocks.first().text, "after = 현재 text")
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
            if (id == edgeId) EdgeDetail(edgeId, 1L, sourceDocId, targetDocId, "criterion") else null
        }
        findDocument.behavior = { id ->
            when (id) {
                sourceDocId -> DocumentDetail(id, "p-$id", "src", DocumentType.MEETING_NOTES, null, null, null, null, null, sourceBlocks)
                targetDocId -> DocumentDetail(id, "p-$id", "tgt", DocumentType.REQUIREMENTS, null, null, null, null, null, targetBlocks)
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
