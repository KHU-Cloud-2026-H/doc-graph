package com.docgraph.backend.validation.command.interfaces.scheduler

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
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.event.EventListener
import java.time.Duration
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ValidationTaskQueuedEventCaptureProbe {
    val latch = CountDownLatch(1)

    @Volatile var received: ValidationTaskQueuedEvent? = null

    @EventListener
    fun on(event: ValidationTaskQueuedEvent) {
        received = event
        latch.countDown()
    }
}

@TestConfiguration
class ValidationTaskStaleSchedulerTestConfig {
    @Bean
    fun validationTaskReadyEventCaptureProbe() = ValidationTaskQueuedEventCaptureProbe()
}

@Tag("component")
@SpringBootTest
@Import(ValidationTaskStaleSchedulerTestConfig::class, SharedPostgresContainer::class)
class ValidationTaskStaleSchedulerTest {

    @Autowired lateinit var scheduler: ValidationTaskStaleScheduler

    @Autowired lateinit var repository: ValidationTaskRepository

    @Autowired lateinit var em: EntityManager

    @Autowired lateinit var txTemplate: TransactionTemplate

    @Autowired lateinit var probe: ValidationTaskQueuedEventCaptureProbe

    // 본 테스트는 scheduler의 publish만 검증. 후속 phase 3 listener 처리는 별 axis라 mock으로 봉인.
    @MockkBean lateinit var processHandler: ProcessValidationTaskCommandHandler

    @BeforeEach
    fun reset() {
        txTemplate.executeWithoutResult {
            em.createQuery("DELETE FROM ValidationTask").executeUpdate()
        }
    }

    @Test
    fun `stale 임계 이전의 PENDING row → ReadyEvent 재발화`() {
        val staleTime = OffsetDateTime.now().minus(Duration.ofMinutes(10))
        val task = repository.save(
            ValidationTask(
                validationPairId = UUID.randomUUID(),
                edgeId = 100L,
                lastAttemptAt = staleTime,
                attempts = 1,
            ),
        )

        scheduler.processStaleRows(Duration.ofMinutes(5))

        val fired = probe.latch.await(5, TimeUnit.SECONDS)
        assertTrue(fired, "stale 임계 초과한 PENDING row가 ReadyEvent 재발화에 누락됨")
        assertEquals(task.id, probe.received?.taskId)
    }
}