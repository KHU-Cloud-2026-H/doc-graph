package com.docgraph.backend.validation.command.domain

import com.docgraph.backend.event.OutboxStatus
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Tag("unit")
class ValidationTaskTest {

    @Test
    fun `recordAttempt — attempts 증가 + lastAttemptAt 갱신`() {
        val task = ValidationTask(validationPairId = UUID.randomUUID(), edgeId = 100L)
        assertEquals(0, task.attempts)
        assertNull(task.lastAttemptAt)

        task.recordAttempt()

        assertEquals(1, task.attempts)
        assertNotNull(task.lastAttemptAt)

        task.recordAttempt()
        assertEquals(2, task.attempts)
    }

    @Test
    fun `markFailed — status FAILED + category·failureReason 기록`() {
        val task = ValidationTask(validationPairId = UUID.randomUUID(), edgeId = 100L)

        task.markFailed(FailureCategory.RATE_LIMITED, "test reason")

        assertEquals(OutboxStatus.FAILED, task.status)
        assertEquals(FailureCategory.RATE_LIMITED, task.failureCategory)
        assertEquals("test reason", task.failureReason)
    }

    @Test
    fun `markFailed — null reason 허용 (category는 유지)`() {
        val task = ValidationTask(validationPairId = UUID.randomUUID(), edgeId = 100L)

        task.markFailed(FailureCategory.UNKNOWN, null)

        assertEquals(OutboxStatus.FAILED, task.status)
        assertEquals(FailureCategory.UNKNOWN, task.failureCategory)
        assertNull(task.failureReason)
    }

    @Test
    fun `markFailed — 최대 길이 초과 reason은 컬럼 길이로 절단`() {
        val task = ValidationTask(validationPairId = UUID.randomUUID(), edgeId = 100L)

        task.markFailed(FailureCategory.UPSTREAM_ERROR, "x".repeat(ValidationTask.FAILURE_REASON_MAX_LENGTH + 500))

        assertEquals(ValidationTask.FAILURE_REASON_MAX_LENGTH, task.failureReason?.length)
    }

    @Test
    fun `markSuccess — category·failureReason 초기화`() {
        val task = ValidationTask(validationPairId = UUID.randomUUID(), edgeId = 100L)
        task.markFailed(FailureCategory.TIMEOUT, "earlier failure")

        task.markSuccess()

        assertEquals(OutboxStatus.SUCCESS, task.status)
        assertNull(task.failureCategory)
        assertNull(task.failureReason)
    }
}
