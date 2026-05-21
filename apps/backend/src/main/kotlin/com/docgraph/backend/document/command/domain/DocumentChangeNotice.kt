package com.docgraph.backend.document.command.domain

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
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime

@Entity
@Table(name = "document_change_notice")
class DocumentChangeNotice(
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

    @Enumerated(EnumType.STRING)
    @Column(name = "change_kind", nullable = false, length = 30)
    val changeKind: DocumentChangeKind,

    @Column(name = "project_id")
    val projectId: Long? = null,

    @Column(name = "notion_event_id", unique = true, length = 100)
    val notionEventId: String? = null,

    @Column(name = "event_type", length = 100)
    val eventType: String? = null,

    @Column(name = "notion_workspace_id", length = 100)
    val notionWorkspaceId: String? = null,

    @Column(name = "subscription_id", length = 100)
    val subscriptionId: String? = null,

    @Column(name = "notion_page_id", nullable = false, length = 100)
    val notionPageId: String,

    @Column(name = "entity_type", nullable = false, length = 30)
    val entityType: String = "page",

    @Column(name = "actor_type", length = 30)
    val actorType: String? = null,

    @Column(name = "attempt_number")
    val attemptNumber: Int? = null,

    @Column(name = "occurred_at", nullable = false)
    val occurredAt: OffsetDateTime,

    @Column(name = "received_at", nullable = false)
    val receivedAt: OffsetDateTime = OffsetDateTime.now(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    val rawPayload: String? = null,
) : OutboxEntry {

    fun recordAttempt() {
        attempts++
        lastAttemptAt = OffsetDateTime.now()
    }

    fun markSuccess() {
        status = OutboxStatus.SUCCESS
        failureReason = null
    }

    fun markFailed(reason: String?) {
        status = OutboxStatus.FAILED
        failureReason = reason
    }
}
