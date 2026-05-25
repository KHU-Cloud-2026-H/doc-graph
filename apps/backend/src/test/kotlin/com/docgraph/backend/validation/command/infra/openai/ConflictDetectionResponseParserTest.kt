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
    fun `정상 — 1건 반환, snake_case → camelCase 매핑`() {
        val json = """{"conflicts":[{"source_block_ids":["a"],"target_block_id":"b","rationale":"r","new_text":"s","title":"t"}]}"""
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
        val json = """
            {"conflicts":[
              {"source_block_ids":["a","b"],"target_block_id":"c","rationale":"r1","new_text":"s1","title":"t1"},
              {"source_block_ids":["d"],"target_block_id":"e","rationale":"r2","new_text":"s2","title":"t2"}
            ]}
        """.trimIndent()
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
    fun `conflicts 빈 배열 — emptyList`() {
        val json = """{"conflicts":[]}"""
        assertTrue(parser.parse(json).isEmpty())
    }

    @Test
    fun `잘못된 JSON 문자열 — 예외`() {
        assertThrows(Exception::class.java) { parser.parse("not json") }
    }

    @Test
    fun `conflicts 키 없음 — 예외 (스키마 위반)`() {
        assertThrows(Exception::class.java) { parser.parse("""{"foo":[]}""") }
    }

    @Test
    fun `필수 필드 누락 — 예외`() {
        val json = """{"conflicts":[{"source_block_ids":["a"]}]}"""
        assertThrows(Exception::class.java) { parser.parse(json) }
    }

    @Test
    fun `new_text 누락 — 예외 (Structured Outputs strict 보호막 우회 시)`() {
        val json = """{"conflicts":[{"source_block_ids":["a"],"target_block_id":"b","rationale":"r"}]}"""
        assertThrows(Exception::class.java) { parser.parse(json) }
    }
}
