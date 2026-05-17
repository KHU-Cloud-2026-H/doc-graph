package com.docgraph.backend.validation.command.infra.openai

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Tag("unit")
class ConflictDetectionResponseSchemaTest {

    @Test
    fun `responseFormat — type은 json_schema`() {
        val rf = ConflictDetectionResponseSchema.responseFormat()
        assertEquals("json_schema", rf["type"])
    }

    @Test
    fun `json_schema — strict=true 보장 (Structured Outputs)`() {
        val js = jsonSchemaOf(ConflictDetectionResponseSchema.responseFormat())
        assertEquals(true, js["strict"])
    }

    @Test
    fun `json_schema — name 존재`() {
        val js = jsonSchemaOf(ConflictDetectionResponseSchema.responseFormat())
        assertNotNull(js["name"])
        assertEquals(true, (js["name"] as String).isNotBlank())
    }

    @Test
    fun `schema 루트 — object, additionalProperties false, required=conflicts`() {
        val schema = schemaOf(ConflictDetectionResponseSchema.responseFormat())
        assertEquals("object", schema["type"])
        assertEquals(false, schema["additionalProperties"])
        @Suppress("UNCHECKED_CAST")
        assertEquals(listOf("conflicts"), schema["required"] as List<String>)
    }

    @Test
    fun `conflicts — array of objects`() {
        val schema = schemaOf(ConflictDetectionResponseSchema.responseFormat())
        @Suppress("UNCHECKED_CAST")
        val props = schema["properties"] as Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val conflicts = props["conflicts"] as Map<String, Any>
        assertEquals("array", conflicts["type"])
        @Suppress("UNCHECKED_CAST")
        val item = conflicts["items"] as Map<String, Any>
        assertEquals("object", item["type"])
        assertEquals(false, item["additionalProperties"])
    }

    @Test
    fun `conflict item — source_block_ids·target_block_ids·rationale·suggestion 모두 required`() {
        val item = conflictItemOf(ConflictDetectionResponseSchema.responseFormat())
        @Suppress("UNCHECKED_CAST")
        val required = (item["required"] as List<String>).toSet()
        assertEquals(setOf("source_block_ids", "target_block_ids", "rationale", "suggestion"), required)
    }

    @Test
    fun `source_block_ids — array of string`() {
        val props = conflictItemPropsOf(ConflictDetectionResponseSchema.responseFormat())
        @Suppress("UNCHECKED_CAST")
        val src = props["source_block_ids"] as Map<String, Any>
        assertEquals("array", src["type"])
        @Suppress("UNCHECKED_CAST")
        val items = src["items"] as Map<String, Any>
        assertEquals("string", items["type"])
    }

    @Test
    fun `target_block_ids — array of string`() {
        val props = conflictItemPropsOf(ConflictDetectionResponseSchema.responseFormat())
        @Suppress("UNCHECKED_CAST")
        val tgt = props["target_block_ids"] as Map<String, Any>
        assertEquals("array", tgt["type"])
        @Suppress("UNCHECKED_CAST")
        val items = tgt["items"] as Map<String, Any>
        assertEquals("string", items["type"])
    }

    @Test
    fun `rationale — string`() {
        val props = conflictItemPropsOf(ConflictDetectionResponseSchema.responseFormat())
        @Suppress("UNCHECKED_CAST")
        val rat = props["rationale"] as Map<String, Any>
        assertEquals("string", rat["type"])
    }

    @Test
    fun `suggestion — string`() {
        val props = conflictItemPropsOf(ConflictDetectionResponseSchema.responseFormat())
        @Suppress("UNCHECKED_CAST")
        val sug = props["suggestion"] as Map<String, Any>
        assertEquals("string", sug["type"])
    }

    @Suppress("UNCHECKED_CAST")
    private fun jsonSchemaOf(rf: Map<String, Any>): Map<String, Any> =
        rf["json_schema"] as Map<String, Any>

    @Suppress("UNCHECKED_CAST")
    private fun schemaOf(rf: Map<String, Any>): Map<String, Any> =
        jsonSchemaOf(rf)["schema"] as Map<String, Any>

    @Suppress("UNCHECKED_CAST")
    private fun conflictItemOf(rf: Map<String, Any>): Map<String, Any> {
        val props = schemaOf(rf)["properties"] as Map<String, Any>
        val conflicts = props["conflicts"] as Map<String, Any>
        return conflicts["items"] as Map<String, Any>
    }

    @Suppress("UNCHECKED_CAST")
    private fun conflictItemPropsOf(rf: Map<String, Any>): Map<String, Any> =
        conflictItemOf(rf)["properties"] as Map<String, Any>
}