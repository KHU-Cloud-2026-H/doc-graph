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
        return envelope.conflicts.map {
            DetectedConflict(
                sourceBlockIds = it.sourceBlockIds,
                targetBlockIds = it.targetBlockIds,
                rationale = it.rationale,
                suggestion = it.suggestion,
            )
        }
    }

    private data class Envelope(val conflicts: List<RawConflict>)

    private data class RawConflict(
        @JsonProperty("source_block_ids") val sourceBlockIds: List<String>,
        @JsonProperty("target_block_ids") val targetBlockIds: List<String>,
        val rationale: String,
        val suggestion: String,
    )
}
