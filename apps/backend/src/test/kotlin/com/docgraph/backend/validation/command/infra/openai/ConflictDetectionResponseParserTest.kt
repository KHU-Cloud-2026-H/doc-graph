package com.docgraph.backend.validation.command.infra.openai

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Tag("unit")
class ConflictDetectionResponseParserTest {

    private val parser = ConflictDetectionResponseParser(jacksonObjectMapper())

    @Test
    fun `정상 — 1건 반환, rich 스키마 → DetectedConflict 매핑`() {
        val json = envelope(
            inconsistency(
                title = "t",
                reason = "r",
                newText = "s",
                sourceBlockIds = listOf("a"),
                targetBlockId = "b",
            ),
        )
        val result = parser.parse(json)
        assertEquals(1, result.size)
        assertEquals(listOf("a"), result[0].sourceBlockIds)
        assertEquals("b", result[0].targetBlockId)
        assertEquals("r", result[0].rationale)
        assertEquals("s", result[0].newText)
        assertEquals("t", result[0].title)
    }

    @Test
    fun `정상 — 여러 건 + 다중 source_block_ids 보존`() {
        val json = envelope(
            inconsistency(title = "t1", reason = "r1", newText = "s1", sourceBlockIds = listOf("a", "b"), targetBlockId = "c"),
            inconsistency(title = "t2", reason = "r2", newText = "s2", sourceBlockIds = listOf("d"), targetBlockId = "e"),
        )
        val result = parser.parse(json)
        assertEquals(2, result.size)
        assertEquals(listOf("a", "b"), result[0].sourceBlockIds)
        assertEquals("e", result[1].targetBlockId)
        assertEquals("r2", result[1].rationale)
        assertEquals("s1", result[0].newText)
        assertEquals("s2", result[1].newText)
        assertEquals("t1", result[0].title)
        assertEquals("t2", result[1].title)
    }

    @Test
    fun `inconsistencies 빈 배열 — emptyList`() {
        val json = """{"summary":{"total_inconsistencies":0,"critical_count":0,"warning_count":0},"inconsistencies":[]}"""
        assertTrue(parser.parse(json).isEmpty())
    }

    @Test
    fun `잘못된 JSON 문자열 — 예외`() {
        assertThrows(Exception::class.java) { parser.parse("not json") }
    }

    @Test
    fun `summary·inconsistencies 키 없음 — 예외 (스키마 위반)`() {
        assertThrows(Exception::class.java) { parser.parse("""{"foo":[]}""") }
    }

    @Test
    fun `inconsistency 필수 필드 누락 — 예외`() {
        val json = """
            {"summary":{"total_inconsistencies":1,"critical_count":0,"warning_count":0},
             "inconsistencies":[{"severity":"경고","category":"기타","title":"t"}]}
        """.trimIndent()
        assertThrows(Exception::class.java) { parser.parse(json) }
    }

    @Test
    fun `new_text 누락 — 예외 (Structured Outputs strict 보호막 우회 시)`() {
        val json = envelope(
            inconsistency(title = "t", reason = "r", newText = null, sourceBlockIds = listOf("a"), targetBlockId = "b"),
        )
        assertThrows(Exception::class.java) { parser.parse(json) }
    }

    private fun envelope(vararg inconsistencies: String): String {
        val items = inconsistencies.joinToString(",")
        return """
            {"summary":{"total_inconsistencies":${inconsistencies.size},"critical_count":0,"warning_count":0},
             "inconsistencies":[$items]}
        """.trimIndent()
    }

    private fun inconsistency(
        title: String,
        reason: String,
        newText: String?,
        sourceBlockIds: List<String>,
        targetBlockId: String,
    ): String {
        val newTextField = if (newText == null) "" else ""","new_text":"$newText""""
        val sources = sourceBlockIds.joinToString(",") { "\"$it\"" }
        return """
            {"severity":"치명적","category":"정책 충돌","title":"$title",
             "source_evidence":{"block_id":"${sourceBlockIds.firstOrNull() ?: ""}","reference":"ref","text":"src"},
             "target_conflict_block":{"block_id":"$targetBlockId","reference":"ref","text":"tgt"},
             "related_blocks":{"source_block_ids":[$sources],"target_block_ids":["$targetBlockId"]},
             "reason":"$reason","impact":"imp"$newTextField}
        """.trimIndent()
    }
}
