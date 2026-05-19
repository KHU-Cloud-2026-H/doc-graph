package com.docgraph.backend.graph.command.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "dependency_edge")
class DependencyEdge(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @Column(name = "project_id", nullable = false)
    val projectId: Long,

    @Column(name = "source_document_id", nullable = false)
    val sourceDocumentId: Long,

    @Column(name = "target_document_id", nullable = false)
    val targetDocumentId: Long,

    @Column(name = "rule_id")
    val ruleId: Long? = null,

    @Column(name = "validation_criterion", nullable = false, length = 500)
    val validationCriterion: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val source: DependencyEdgeSource,

    @Enumerated(EnumType.STRING)
    @Column(name = "conflict_status", nullable = false, length = 20)
    var conflictStatus: EdgeConflictStatus = EdgeConflictStatus.NONE,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = createdAt,
) {
    init {
        require(sourceDocumentId != targetDocumentId) {
            "sourceDocumentId and targetDocumentId must be different"
        }
        require(validationCriterion.isNotBlank()) {
            "validationCriterion must not be blank"
        }
    }

    fun markConflict(at: OffsetDateTime = OffsetDateTime.now()) {
        conflictStatus = EdgeConflictStatus.CONFLICT
        updatedAt = at
    }

    fun clearConflict(at: OffsetDateTime = OffsetDateTime.now()) {
        conflictStatus = EdgeConflictStatus.NONE
        updatedAt = at
    }
}

enum class DependencyEdgeSource {
    NOTION_REFERENCE,
    PROPOSAL_ACCEPTED,
    CUSTOM,
}

enum class EdgeConflictStatus {
    NONE,
    CONFLICT,
}
