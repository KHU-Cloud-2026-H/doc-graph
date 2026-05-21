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
                targetBlockId = it.targetBlockId,
                rationale = it.rationale,
                newText = it.newText,
            )
        }
    }

    private data class Envelope(val conflicts: List<RawConflict>)

    private data class RawConflict(
        @JsonProperty("source_block_ids") val sourceBlockIds: List<String>,
        @JsonProperty("target_block_id") val targetBlockId: String,
        val rationale: String,
        @JsonProperty("new_text") val newText: String,
    )
}
