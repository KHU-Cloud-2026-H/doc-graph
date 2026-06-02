package com.docgraph.backend.validation.command.infra.openai

object ConflictDetectionResponseSchema {

    fun responseFormat(): Map<String, Any> = mapOf(
        "type" to "json_schema",
        "json_schema" to mapOf(
            "name" to "conflict_detection",
            "strict" to true,
            "schema" to schema(),
        ),
    )

    private fun schema(): Map<String, Any> = mapOf(
        "type" to "object",
        "additionalProperties" to false,
        "required" to listOf("summary", "inconsistencies"),
        "properties" to mapOf(
            "summary" to mapOf(
                "type" to "object",
                "additionalProperties" to false,
                "required" to listOf("total_inconsistencies", "critical_count", "warning_count"),
                "properties" to mapOf(
                    "total_inconsistencies" to mapOf("type" to "integer"),
                    "critical_count" to mapOf("type" to "integer"),
                    "warning_count" to mapOf("type" to "integer"),
                ),
            ),
            "inconsistencies" to mapOf(
                "type" to "array",
                "items" to mapOf(
                    "type" to "object",
                    "additionalProperties" to false,
                    "required" to listOf(
                        "severity",
                        "category",
                        "title",
                        "source_evidence",
                        "target_conflict_block",
                        "related_blocks",
                        "reason",
                        "impact",
                        "new_text",
                    ),
                    "properties" to mapOf(
                        "severity" to mapOf("type" to "string"),
                        "category" to mapOf("type" to "string"),
                        "title" to mapOf("type" to "string"),
                        "source_evidence" to evidence(),
                        "target_conflict_block" to evidence(),
                        "related_blocks" to mapOf(
                            "type" to "object",
                            "additionalProperties" to false,
                            "required" to listOf("source_block_ids", "target_block_ids"),
                            "properties" to mapOf(
                                "source_block_ids" to arrayOfStrings(),
                                "target_block_ids" to arrayOfStrings(),
                            ),
                        ),
                        "reason" to mapOf("type" to "string"),
                        "impact" to mapOf("type" to "string"),
                        "new_text" to mapOf("type" to "string"),
                    ),
                ),
            ),
        ),
    )

    private fun evidence(): Map<String, Any> = mapOf(
        "type" to "object",
        "additionalProperties" to false,
        "required" to listOf("block_id", "reference", "text"),
        "properties" to mapOf(
            "block_id" to mapOf("type" to "string"),
            "reference" to mapOf("type" to "string"),
            "text" to mapOf("type" to "string"),
        ),
    )

    private fun arrayOfStrings(): Map<String, Any> = mapOf(
        "type" to "array",
        "items" to mapOf("type" to "string"),
    )
}
