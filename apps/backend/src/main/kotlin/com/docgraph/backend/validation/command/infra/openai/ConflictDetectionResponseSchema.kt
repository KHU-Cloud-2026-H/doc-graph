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
        "required" to listOf("conflicts"),
        "properties" to mapOf(
            "conflicts" to mapOf(
                "type" to "array",
                "items" to mapOf(
                    "type" to "object",
                    "additionalProperties" to false,
                    "required" to listOf("source_block_ids", "target_block_ids", "rationale", "suggestion"),
                    "properties" to mapOf(
                        "source_block_ids" to arrayOfStrings(),
                        "target_block_ids" to arrayOfStrings(),
                        "rationale" to mapOf("type" to "string"),
                        "suggestion" to mapOf("type" to "string"),
                    ),
                ),
            ),
        ),
    )

    private fun arrayOfStrings(): Map<String, Any> = mapOf(
        "type" to "array",
        "items" to mapOf("type" to "string"),
    )
}
