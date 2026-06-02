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
    fun `schema 루트 — object, additionalProperties false, required=summary·inconsistencies`() {
        val schema = schemaOf(ConflictDetectionResponseSchema.responseFormat())
        assertEquals("object", schema["type"])
        assertEquals(false, schema["additionalProperties"])
        @Suppress("UNCHECKED_CAST")
        assertEquals(listOf("summary", "inconsistencies"), schema["required"] as List<String>)
    }

    @Test
    fun `summary — total·critical·warning count 모두 required integer`() {
        @Suppress("UNCHECKED_CAST")
        val summary = (schemaOf(ConflictDetectionResponseSchema.responseFormat())["properties"] as Map<String, Any>)["summary"] as Map<String, Any>
        assertEquals("object", summary["type"])
        @Suppress("UNCHECKED_CAST")
        assertEquals(
            setOf("total_inconsistencies", "critical_count", "warning_count"),
            (summary["required"] as List<String>).toSet(),
        )
        @Suppress("UNCHECKED_CAST")
        val props = summary["properties"] as Map<String, Any>
        for (key in listOf("total_inconsistencies", "critical_count", "warning_count")) {
            @Suppress("UNCHECKED_CAST")
            assertEquals("integer", (props[key] as Map<String, Any>)["type"], "$key 는 integer")
        }
    }

    @Test
    fun `inconsistencies — array of objects`() {
        val schema = schemaOf(ConflictDetectionResponseSchema.responseFormat())
        @Suppress("UNCHECKED_CAST")
        val props = schema["properties"] as Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val inconsistencies = props["inconsistencies"] as Map<String, Any>
        assertEquals("array", inconsistencies["type"])
        @Suppress("UNCHECKED_CAST")
        val item = inconsistencies["items"] as Map<String, Any>
        assertEquals("object", item["type"])
        assertEquals(false, item["additionalProperties"])
    }

    @Test
    fun `inconsistency item — 9개 필드 모두 required`() {
        val item = inconsistencyItemOf(ConflictDetectionResponseSchema.responseFormat())
        @Suppress("UNCHECKED_CAST")
        val required = (item["required"] as List<String>).toSet()
        assertEquals(
            setOf(
                "severity", "category", "title",
                "source_evidence", "target_conflict_block", "related_blocks",
                "reason", "impact", "new_text",
            ),
            required,
        )
    }

    @Test
    fun `inconsistency item — 단순 string 필드 타입`() {
        val props = inconsistencyItemPropsOf(ConflictDetectionResponseSchema.responseFormat())
        for (key in listOf("severity", "category", "title", "reason", "impact", "new_text")) {
            @Suppress("UNCHECKED_CAST")
            assertEquals("string", (props[key] as Map<String, Any>)["type"], "$key 는 string")
        }
    }

    @Test
    fun `source_evidence·target_conflict_block — block_id·reference·text string object`() {
        val props = inconsistencyItemPropsOf(ConflictDetectionResponseSchema.responseFormat())
        for (key in listOf("source_evidence", "target_conflict_block")) {
            @Suppress("UNCHECKED_CAST")
            val evidence = props[key] as Map<String, Any>
            assertEquals("object", evidence["type"], "$key 는 object")
            @Suppress("UNCHECKED_CAST")
            assertEquals(setOf("block_id", "reference", "text"), (evidence["required"] as List<String>).toSet())
            @Suppress("UNCHECKED_CAST")
            val ep = evidence["properties"] as Map<String, Any>
            for (field in listOf("block_id", "reference", "text")) {
                @Suppress("UNCHECKED_CAST")
                assertEquals("string", (ep[field] as Map<String, Any>)["type"])
            }
        }
    }

    @Test
    fun `related_blocks — source·target block_ids array of string`() {
        val props = inconsistencyItemPropsOf(ConflictDetectionResponseSchema.responseFormat())
        @Suppress("UNCHECKED_CAST")
        val related = props["related_blocks"] as Map<String, Any>
        assertEquals("object", related["type"])
        @Suppress("UNCHECKED_CAST")
        val rp = related["properties"] as Map<String, Any>
        for (key in listOf("source_block_ids", "target_block_ids")) {
            @Suppress("UNCHECKED_CAST")
            val arr = rp[key] as Map<String, Any>
            assertEquals("array", arr["type"], "$key 는 array")
            @Suppress("UNCHECKED_CAST")
            assertEquals("string", (arr["items"] as Map<String, Any>)["type"])
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun jsonSchemaOf(rf: Map<String, Any>): Map<String, Any> =
        rf["json_schema"] as Map<String, Any>

    @Suppress("UNCHECKED_CAST")
    private fun schemaOf(rf: Map<String, Any>): Map<String, Any> =
        jsonSchemaOf(rf)["schema"] as Map<String, Any>

    @Suppress("UNCHECKED_CAST")
    private fun inconsistencyItemOf(rf: Map<String, Any>): Map<String, Any> {
        val props = schemaOf(rf)["properties"] as Map<String, Any>
        val inconsistencies = props["inconsistencies"] as Map<String, Any>
        return inconsistencies["items"] as Map<String, Any>
    }

    @Suppress("UNCHECKED_CAST")
    private fun inconsistencyItemPropsOf(rf: Map<String, Any>): Map<String, Any> =
        inconsistencyItemOf(rf)["properties"] as Map<String, Any>
}
