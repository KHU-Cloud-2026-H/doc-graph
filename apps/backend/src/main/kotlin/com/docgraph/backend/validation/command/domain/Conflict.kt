package com.docgraph.backend.validation.command.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "conflict")
class Conflict(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @Column(name = "edge_id", nullable = false, updatable = false)
    val edgeId: Long,

    @Column(name = "first_detected_at", nullable = false, updatable = false)
    val firstDetectedAt: OffsetDateTime,

    @Column(name = "last_detected_at", nullable = false)
    var lastDetectedAt: OffsetDateTime,

    @Column(name = "resolved_at")
    var resolvedAt: OffsetDateTime? = null,

    @Column(name = "ignored_at")
    var ignoredAt: OffsetDateTime? = null,

    @Column(name = "ignored_by")
    var ignoredBy: Long? = null,

    @Column(name = "ignore_reason", length = 500)
    var ignoreReason: String? = null,
) {
    fun markDetected(at: OffsetDateTime) {
        lastDetectedAt = at
        clearIgnore()
    }

    fun markResolved(at: OffsetDateTime) {
        resolvedAt = at
    }

    fun ignore(by: Long, reason: String?, at: OffsetDateTime) {
        ignoredAt = at
        ignoredBy = by
        ignoreReason = reason
    }

    fun unignore() {
        clearIgnore()
    }

    val isActive: Boolean get() = resolvedAt == null
    val isIgnored: Boolean get() = ignoredAt != null

    private fun clearIgnore() {
        ignoredAt = null
        ignoredBy = null
        ignoreReason = null
    }
}