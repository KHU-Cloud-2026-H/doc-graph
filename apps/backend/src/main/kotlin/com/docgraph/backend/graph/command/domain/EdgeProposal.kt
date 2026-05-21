package com.docgraph.backend.graph.command.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "edge_proposal")
class EdgeProposal(
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

    @Column(name = "similarity_score", nullable = false)
    val similarityScore: Double,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
) {
    init {
        require(sourceDocumentId != targetDocumentId) {
            "sourceDocumentId and targetDocumentId must be different"
        }
        require(validationCriterion.isNotBlank()) {
            "validationCriterion must not be blank"
        }
        require(similarityScore in 0.0..1.0) {
            "similarityScore must be between 0.0 and 1.0"
        }
    }

    /**
     * Accepts this proposal and returns a new DependencyEdge.
     *
     * Note: The application service must handle the proposal lifecycle
     * by saving the edge and deleting this proposal in a single transaction
     * to prevent duplicate edge/proposal with the same direction.
     */
    fun accept(at: OffsetDateTime = OffsetDateTime.now()): DependencyEdge =
        DependencyEdge(
            projectId = projectId,
            sourceDocumentId = sourceDocumentId,
            targetDocumentId = targetDocumentId,
            ruleId = ruleId,
            validationCriterion = validationCriterion,
            source = DependencyEdgeSource.PROPOSAL_ACCEPTED,
            createdAt = at,
            updatedAt = at,
        )
}
