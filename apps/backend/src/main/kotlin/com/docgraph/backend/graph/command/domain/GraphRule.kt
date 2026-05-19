package com.docgraph.backend.graph.command.domain

import com.docgraph.backend.document.query.application.DocumentType
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
@Table(name = "graph_rule")
class GraphRule(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @Column(name = "project_id")
    val projectId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    val sourceType: DocumentType,

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 50)
    val targetType: DocumentType,

    @Column(name = "validation_criterion", nullable = false, length = 500)
    val validationCriterion: String,

    @Column(name = "is_default", nullable = false)
    val isDefault: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
) {
    init {
        require(sourceType != targetType) {
            "sourceType and targetType must be different"
        }
        require(validationCriterion.isNotBlank()) {
            "validationCriterion must not be blank"
        }
        require((isDefault && projectId == null) || (!isDefault && projectId != null)) {
            "default rule must be global and custom rule must belong to a project"
        }
    }

    fun appliesTo(sourceType: DocumentType?, targetType: DocumentType?): Boolean =
        this.sourceType == sourceType && this.targetType == targetType

    fun toEdge(
        projectId: Long,
        sourceDocumentId: Long,
        targetDocumentId: Long,
        source: DependencyEdgeSource,
        at: OffsetDateTime = OffsetDateTime.now(),
    ): DependencyEdge = DependencyEdge(
        projectId = projectId,
        sourceDocumentId = sourceDocumentId,
        targetDocumentId = targetDocumentId,
        ruleId = id.takeIf { it != 0L },
        validationCriterion = validationCriterion,
        source = source,
        createdAt = at,
        updatedAt = at,
    )
}
