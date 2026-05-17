package com.docgraph.backend.validation.command.interfaces.event

import com.docgraph.backend.document.query.application.DocumentDetail
import com.docgraph.backend.document.query.application.DocumentType
import com.docgraph.backend.document.query.application.FindDocumentByIdQuery
import com.docgraph.backend.event.OutboxStatus
import com.docgraph.backend.graph.command.domain.ValidationPairCreatedEvent
import com.docgraph.backend.graph.query.application.EdgeDetail
import com.docgraph.backend.graph.query.application.FindEdgeByIdQuery
import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.validation.command.domain.ValidationTask
import com.docgraph.backend.validation.command.domain.ValidationTaskPreparedEvent
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
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FlowFakeFindEdgeByIdQuery : FindEdgeByIdQuery {
    @Volatile var behavior: (Long) -> EdgeDetail? = { null }
    override fun find(edgeId: Long): EdgeDetail? = behavior(edgeId)
}

class FlowFakeFindDocumentByIdQuery : FindDocumentByIdQuery {
    @Volatile var behavior: (Long) -> DocumentDetail? = { null }
    override fun find(documentId: Long): DocumentDetail? = behavior(documentId)
}

class FlowPreparedProbe {
    val latch = CountDownLatch(1)
    @Volatile var received: ValidationTaskPreparedEvent? = null

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: ValidationTaskPreparedEvent) {
        received = event
        latch.countDown()
    }
}

open class FlowTestPublisher(private val publisher: ApplicationEventPublisher) {
    @Transactional
    open fun publish(event: ValidationPairCreatedEvent) {
        publisher.publishEvent(event)
    }
}

@TestConfiguration
class ValidationTaskFlowTestConfig {
    @Bean @Primary
    fun flowFakeFindEdgeByIdQuery() = FlowFakeFindEdgeByIdQuery()

    @Bean @Primary
    fun flowFakeFindDocumentByIdQuery() = FlowFakeFindDocumentByIdQuery()

    @Bean
    fun flowPreparedProbe() = FlowPreparedProbe()

    @Bean
    fun flowTestPublisher(publisher: ApplicationEventPublisher) = FlowTestPublisher(publisher)
}

@Tag("component")
@SpringBootTest
@Import(ValidationTaskFlowTestConfig::class, SharedPostgresContainer::class)
class ValidationTaskFlowTest {

    @Autowired lateinit var testPublisher: FlowTestPublisher

    @Autowired lateinit var probe: FlowPreparedProbe

    @Autowired lateinit var em: EntityManager

    @Autowired lateinit var txTemplate: TransactionTemplate

    @Autowired lateinit var findEdge: FlowFakeFindEdgeByIdQuery

    @Autowired lateinit var findDocument: FlowFakeFindDocumentByIdQuery

    @BeforeEach
    fun reset() {
        txTemplate.executeWithoutResult {
            em.createQuery("DELETE FROM ValidationTask").executeUpdate()
        }
    }

    @Test
    fun `통합 — ValidationPairCreatedEvent에서 ValidationTaskPreparedEvent까지 한 흐름`() {
        val pairId = UUID.randomUUID()
        val edgeId = 100L
        val sourceDocId = 200L
        val targetDocId = 300L

        findEdge.behavior = { id ->
            if (id == edgeId) EdgeDetail(edgeId, sourceDocId, targetDocId, "criterion") else null
        }
        findDocument.behavior = { id ->
            DocumentDetail(id, "page-$id", "title-$id", DocumentType.MEETING_NOTES, null, null, emptyList())
        }

        testPublisher.publish(ValidationPairCreatedEvent(pairId, edgeId, OffsetDateTime.now()))

        val fired = probe.latch.await(10, TimeUnit.SECONDS)
        assertTrue(fired, "ValidationTaskPreparedEvent did not fire within 10s — 흐름 어딘가에서 중단")

        val tasks = em.createQuery("SELECT t FROM ValidationTask t", ValidationTask::class.java).resultList
        assertEquals(1, tasks.size)
        val task = tasks.single()

        assertEquals(task.id, probe.received?.validationTaskId)
        assertEquals(pairId, task.validationPairId)
        assertEquals(edgeId, task.edgeId)
        assertEquals(OutboxStatus.PENDING, task.status, "처리 직전 상태 — status는 PENDING 유지")
        assertEquals(1, task.attempts)
        assertNotNull(task.lastAttemptAt)
    }
}
