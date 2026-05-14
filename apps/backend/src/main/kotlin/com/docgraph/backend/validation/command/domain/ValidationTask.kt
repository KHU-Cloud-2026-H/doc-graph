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

    @Column(name = "failure_reason", length = 1000)
    override var failureReason: String? = null,

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

    fun markFailed(reason: String?) {
        status = OutboxStatus.FAILED
        failureReason = reason
    }

    fun markSuccess() {
        status = OutboxStatus.SUCCESS
        failureReason = null
    }
}