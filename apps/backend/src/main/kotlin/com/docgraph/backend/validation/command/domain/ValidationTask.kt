package com.docgraph.backend.validation.command.domain

import com.docgraph.backend.event.OutboxEntry
import com.docgraph.backend.event.OutboxStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "validation_task")
class ValidationTask(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    override var id: Long = 0L,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    override var status: OutboxStatus = OutboxStatus.PENDING,

    @Column(nullable = false)
    override var attempts: Int = 0,

    @Column(name = "last_attempt_at")
    override var lastAttemptAt: OffsetDateTime? = null,

    @Column(name = "failure_reason", length = FAILURE_REASON_MAX_LENGTH)
    override var failureReason: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_category", length = 30)
    var failureCategory: FailureCategory? = null,

    @Column(nullable = false, unique = true, columnDefinition = "uuid")
    val validationPairId: UUID,

    @Column(nullable = false)
    val edgeId: Long,

    @Column(nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
) : OutboxEntry {

    fun recordAttempt() {
        attempts++
        lastAttemptAt = OffsetDateTime.now()
    }

    fun markFailed(category: FailureCategory, reason: String?) {
        status = OutboxStatus.FAILED
        failureCategory = category
        // 외부 API 에러 메시지(긴 본문)가 그대로 넘어올 수 있어 컬럼 길이로 절단.
        failureReason = reason?.take(FAILURE_REASON_MAX_LENGTH)
    }

    fun markSuccess() {
        status = OutboxStatus.SUCCESS
        failureCategory = null
        failureReason = null
    }

    companion object {
        const val FAILURE_REASON_MAX_LENGTH = 1000
    }
}