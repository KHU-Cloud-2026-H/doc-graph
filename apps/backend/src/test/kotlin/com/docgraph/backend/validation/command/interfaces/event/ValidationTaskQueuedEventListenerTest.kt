package com.docgraph.backend.validation.command.interfaces.event

import com.docgraph.backend.document.query.application.DocumentDetail
import com.docgraph.backend.document.query.application.DocumentType
import com.docgraph.backend.document.query.application.FindDocumentByIdQuery
import com.docgraph.backend.event.OutboxStatus
import com.docgraph.backend.graph.query.application.EdgeDetail
import com.docgraph.backend.graph.query.application.FindEdgeByIdQuery
import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.validation.command.domain.ValidationTask
import com.docgraph.backend.validation.command.domain.ValidationTaskPreparedEvent
import com.docgraph.backend.validation.command.domain.ValidationTaskQueuedEvent
import com.docgraph.backend.validation.command.domain.ValidationTaskRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.springframework.transaction.support.TransactionTemplate
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FakeFindEdgeByIdQuery : FindEdgeByIdQuery {
    @Volatile var behavior: (Long) -> EdgeDetail? = { null }
    val callCount = AtomicInteger(0)

    override fun find(edgeId: Long): EdgeDetail? {
        callCount.incrementAndGet()
        return behavior(edgeId)
    }

    fun reset() {
        behavior = { null }
        callCount.set(0)
    }
}

class FakeFindDocumentByIdQuery : FindDocumentByIdQuery {
    @Volatile var behavior: (Long) -> DocumentDetail? = { null }
    val callCount = AtomicInteger(0)

    override fun find(documentId: Long): DocumentDetail? {
        callCount.incrementAndGet()
        return behavior(documentId)
    }

    fun reset() {
        behavior = { null }
        callCount.set(0)
    }
}

class ValidationTaskPreparedProbe {
    @Volatile var latch: CountDownLatch = CountDownLatch(1)
    @Volatile var received: ValidationTaskPreparedEvent? = null

    fun reset() {
        latch = CountDownLatch(1)
        received = null
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: ValidationTaskPreparedEvent) {
        received = event
        latch.countDown()
    }
}

open class ReadyEventTestPublisher(private val publisher: ApplicationEventPublisher) {
    @Transactional
    open fun publish(event: ValidationTaskQueuedEvent) {
        publisher.publishEvent(event)
    }
}

@TestConfiguration
class ValidationTaskQueuedEventListenerTestConfig {
    @Bean @Primary
    fun fakeFindEdgeByIdQuery() = FakeFindEdgeByIdQuery()

    @Bean @Primary
    fun fakeFindDocumentByIdQuery() = FakeFindDocumentByIdQuery()

    @Bean
    fun validationTaskPreparedProbe() = ValidationTaskPreparedProbe()

    @Bean
    fun readyEventTestPublisher(publisher: ApplicationEventPublisher) =
        ReadyEventTestPublisher(publisher)
}

@Tag("component")
@SpringBootTest(
    properties = [
        "validation.task.process.max-attempts=2",
        "validation.task.process.retry-delay-ms=10",
        "validation.task.process.retry-multiplier=1.0",
        "validation.task.process.retry-max-delay-ms=10",
    ],
)
@Import(ValidationTaskQueuedEventListenerTestConfig::class, SharedPostgresContainer::class)
class ValidationTaskQueuedEventListenerTest {

    @Autowired lateinit var testPublisher: ReadyEventTestPublisher

    @Autowired lateinit var repository: ValidationTaskRepository

    @Autowired lateinit var em: EntityManager

    @Autowired lateinit var txTemplate: TransactionTemplate

    @Autowired lateinit var probe: ValidationTaskPreparedProbe

    @Autowired lateinit var findEdge: FakeFindEdgeByIdQuery

    @Autowired lateinit var findDocument: FakeFindDocumentByIdQuery

    @BeforeEach
    fun reset() {
        txTemplate.executeWithoutResult {
            em.createQuery("DELETE FROM ValidationTask").executeUpdate()
        }
        probe.reset()
        findEdge.reset()
        findDocument.reset()
    }

    @Test
    fun `정상 흐름 — 두 문서 조회 후 ValidationTaskPrepared 발행, status PENDING 유지`() {
        val edgeId = 100L
        val sourceDocId = 200L
        val targetDocId = 300L
        findEdge.behavior = { id ->
            if (id == edgeId) EdgeDetail(edgeId, 1L, sourceDocId, targetDocId, "criterion") else null
        }
        findDocument.behavior = { id ->
            DocumentDetail(id, "page-$id", "title-$id", DocumentType.MEETING_NOTES, null, null, null, null, emptyList())
        }

        val task = repository.save(ValidationTask(validationPairId = UUID.randomUUID(), edgeId = edgeId))

        testPublisher.publish(ValidationTaskQueuedEvent(task.id))

        val fired = probe.latch.await(5, TimeUnit.SECONDS)
        assertTrue(fired, "ValidationTaskPreparedEvent did not fire within 5s")

        awaitCondition(Duration.ofSeconds(5)) {
            findEdge.callCount.get() == 2 && findDocument.callCount.get() == 4
        }

        assertEquals(task.id, probe.received?.validationTaskId)
        assertEquals(2, findEdge.callCount.get(), "FindEdge 2회 호출 기대 (Process handler + Prepared listener)")
        assertEquals(4, findDocument.callCount.get(), "FindDocument 4회 호출 기대 (Process + Prepared 각각 source+target)")

        val updated = repository.findById(task.id)!!
        assertEquals(OutboxStatus.PENDING, updated.status, "처리 직전 단계 — status PENDING 유지")
        assertEquals(1, updated.attempts)
        assertNotNull(updated.lastAttemptAt)
    }

    @Test
    fun `한 task 실패가 다른 task 처리에 영향 없음`() {
        val edgeIdOk = 100L
        val edgeIdFail = 999L
        findEdge.behavior = { id ->
            when (id) {
                edgeIdOk -> EdgeDetail(id, 1L, 200L, 300L, "criterion")
                edgeIdFail -> throw RuntimeException("forced-failure")
                else -> null
            }
        }
        findDocument.behavior = { id ->
            DocumentDetail(id, "page-$id", "t-$id", DocumentType.MEETING_NOTES, null, null, null, null, emptyList())
        }

        val taskOk = repository.save(ValidationTask(validationPairId = UUID.randomUUID(), edgeId = edgeIdOk))
        val taskFail = repository.save(ValidationTask(validationPairId = UUID.randomUUID(), edgeId = edgeIdFail))

        testPublisher.publish(ValidationTaskQueuedEvent(taskOk.id))
        testPublisher.publish(ValidationTaskQueuedEvent(taskFail.id))

        awaitTaskStatus(taskFail.id, OutboxStatus.FAILED, Duration.ofSeconds(5))

        // taskOk 처리 완료 시점은 latch와 별개 — async tx commit 대기 buffer
        Thread.sleep(500)

        val updatedOk = repository.findById(taskOk.id)!!
        assertEquals(OutboxStatus.PENDING, updatedOk.status, "정상 task가 격리 미준수로 영향 — REQUIRES_NEW 미적용 의심")
        assertEquals(1, updatedOk.attempts)
    }

    private fun awaitTaskStatus(taskId: Long, expected: OutboxStatus, timeout: Duration) {
        val deadline = System.currentTimeMillis() + timeout.toMillis()
        while (System.currentTimeMillis() < deadline) {
            val task = repository.findById(taskId)
            if (task?.status == expected) return
            Thread.sleep(50)
        }
        throw AssertionError("task $taskId did not reach status $expected within $timeout")
    }

    private fun awaitCondition(timeout: Duration, predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeout.toMillis()
        while (System.currentTimeMillis() < deadline) {
            if (predicate()) return
            Thread.sleep(50)
        }
        throw AssertionError("condition not satisfied within $timeout")
    }
}
