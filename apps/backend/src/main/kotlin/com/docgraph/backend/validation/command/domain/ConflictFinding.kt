package com.docgraph.backend.validation.command.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime

@Entity
@Table(name = "conflict_finding")
class ConflictFinding(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @Column(name = "conflict_id", nullable = false, updatable = false)
    val conflictId: Long,

    @Column(name = "validation_task_id", nullable = false, updatable = false)
    val validationTaskId: Long,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_block_ids", nullable = false, columnDefinition = "jsonb")
    val sourceBlockIds: List<String>,

    @Column(name = "target_block_id", nullable = false, columnDefinition = "text")
    val targetBlockId: String,

    @Column(name = "rationale", nullable = false, columnDefinition = "text")
    val rationale: String,

    @Column(name = "new_text", nullable = false, columnDefinition = "text")
    val newText: String,

    @Column(name = "detected_at", nullable = false, updatable = false)
    val detectedAt: OffsetDateTime,

    @Column(name = "approved_at")
    var approvedAt: OffsetDateTime? = null,

    @Column(name = "approved_by")
    var approvedBy: Long? = null,
) {
    val isApproved: Boolean get() = approvedAt != null

    fun approve(by: Long, at: OffsetDateTime) {
        if (isApproved) {
            throw IllegalConflictFindingStateException(id, "이미 승인된 finding")
        }
        approvedAt = at
        approvedBy = by
    }
}