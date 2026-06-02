package com.docgraph.backend.validation.command.infra.openai

import com.docgraph.backend.validation.command.domain.DetectedConflict
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

@Component
class ConflictDetectionResponseParser(private val mapper: ObjectMapper) {

    fun parse(content: String): List<DetectedConflict> {
        val envelope: Envelope = mapper.readValue(content)
        return envelope.inconsistencies.map {
            DetectedConflict(
                sourceBlockIds = it.relatedBlocks.sourceBlockIds,
                targetBlockId = it.targetConflictBlock.blockId,
                rationale = it.reason,
                newText = it.newText,
                title = it.title,
            )
        }
    }

    private data class Envelope(
        val summary: Summary,
        val inconsistencies: List<RawInconsistency>,
    )

    private data class Summary(
        @JsonProperty("total_inconsistencies") val totalInconsistencies: Int,
        @JsonProperty("critical_count") val criticalCount: Int,
        @JsonProperty("warning_count") val warningCount: Int,
    )

    private data class RawInconsistency(
        val severity: String,
        val category: String,
        val title: String,
        @JsonProperty("source_evidence") val sourceEvidence: Evidence,
        @JsonProperty("target_conflict_block") val targetConflictBlock: Evidence,
        @JsonProperty("related_blocks") val relatedBlocks: RelatedBlocks,
        val reason: String,
        val impact: String,
        @JsonProperty("new_text") val newText: String,
    )

    private data class Evidence(
        @JsonProperty("block_id") val blockId: String,
        val reference: String,
        val text: String,
    )

    private data class RelatedBlocks(
        @JsonProperty("source_block_ids") val sourceBlockIds: List<String>,
        @JsonProperty("target_block_ids") val targetBlockIds: List<String>,
    )
}
