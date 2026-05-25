package com.docgraph.backend.validation.command.interfaces.event

import com.docgraph.backend.event.OutboxStatus
import com.docgraph.backend.graph.command.domain.ValidationPairCreatedEvent
import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.validation.command.application.ProcessValidationTaskCommandHandler
import com.docgraph.backend.validation.command.domain.ValidationTask
import com.docgraph.backend.validation.command.domain.ValidationTaskQueuedEvent
import com.docgraph.backend.validation.command.domain.ValidationTaskRepository
import com.ninjasquad.springmockk.MockkBean
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
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ValidationTaskQueuedProbe {
    @Volatile var latch: CountDownLatch = CountDownLatch(1)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onReady(event: ValidationTaskQueuedEvent) {
        latch.countDown()
    }
}

open class ValidationPairCreatedTestPublisher(
    private val publisher: ApplicationEventPublisher,
) {
    @Transactional
    open fun publish(event: ValidationPairCreatedEvent) {
        publisher.publishEvent(event)
    }
}

@TestConfiguration
class ValidationPairCreatedEventListenerTestConfig {
    @Bean
    fun validationTaskQueuedProbe() = ValidationTaskQueuedProbe()

    @Bean
    fun validationPairCreatedTestPublisher(publisher: ApplicationEventPublisher) =
        ValidationPairCreatedTestPublisher(publisher)
}

@Tag("component")
@SpringBootTest
@Import(ValidationPairCreatedEventListenerTestConfig::class, SharedPostgresContainer::class)
class ValidationPairCreatedEventListenerTest {

    @Autowired lateinit var testPublisher: ValidationPairCreatedTestPublisher

    @Autowired lateinit var probe: ValidationTaskQueuedProbe

    @Autowired lateinit var repository: ValidationTaskRepository

    @Autowired lateinit var em: EntityManager

    @Autowired lateinit var txTemplate: TransactionTemplate

    // phase 3 listener가 ValidationTaskQueuedEvent를 async 처리해 race를 만드므로,
    // phase 1 단위 검증에선 처리 단계를 mock으로 봉인.
    @MockkBean lateinit var processHandler: ProcessValidationTaskCommandHandler

    @BeforeEach
    fun resetState() {
        txTemplate.executeWithoutResult {
            em.createQuery("DELETE FROM ValidationTask").executeUpdate()
        }
        probe.latch = CountDownLatch(1)
    }

    @Test
    fun `persists ValidationTask(pending) when ValidationPairCreatedEvent is published`() {
        val pairId = UUID.randomUUID()
        val edgeId = 42L

        testPublisher.publish(ValidationPairCreatedEvent(pairId, edgeId, OffsetDateTime.now()))

        val ready = probe.latch.await(5, TimeUnit.SECONDS)
        assertTrue(ready, "ValidationTaskQueuedEvent did not fire within 5s — listener가 내부 이벤트를 발행하지 않음")

        val tasks = em.createQuery("SELECT t FROM ValidationTask t", ValidationTask::class.java).resultList
        assertEquals(1, tasks.size, "expected exactly 1 ValidationTask")
        val task = tasks.single()
        assertEquals(pairId, task.validationPairId)
        assertEquals(edgeId, task.edgeId)
        assertEquals(OutboxStatus.PENDING, task.status)
        assertEquals(0, task.attempts)
    }

    @Test
    fun `dedupes by validationPairId when same event is published twice`() {
        val pairId = UUID.randomUUID()
        val edgeId = 42L
        val event = ValidationPairCreatedEvent(pairId, edgeId, OffsetDateTime.now())

        testPublisher.publish(event)
        val firstReady = probe.latch.await(5, TimeUnit.SECONDS)
        assertTrue(firstReady, "first publish: ready event did not fire")

        // 동일 validationPairId 재발행 — listener는 findByValidationPairId 사전 조회로 skip해야 함.
        // skip path는 ready event를 발행하지 않으므로 latch로 신호 받을 수 없음 → 비동기 listener 처리 buffer.
        testPublisher.publish(event)
        Thread.sleep(1500)

        val count = em.createQuery("SELECT COUNT(t) FROM ValidationTask t", Long::class.javaObjectType).singleResult
        assertEquals(1L, count, "duplicate publish가 row 1건을 더 만들었음 — idempotency 미적용")
    }
}