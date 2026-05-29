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
                    "required" to listOf("source_block_ids", "target_block_id", "rationale", "new_text", "title"),
                    "properties" to mapOf(
                        "source_block_ids" to arrayOfStrings(),
                        "target_block_id" to mapOf("type" to "string"),
                        "rationale" to mapOf("type" to "string"),
                        "new_text" to mapOf("type" to "string"),
                        "title" to mapOf("type" to "string"),
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
