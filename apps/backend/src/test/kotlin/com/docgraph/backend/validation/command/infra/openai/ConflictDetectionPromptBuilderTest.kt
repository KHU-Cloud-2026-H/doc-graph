package com.docgraph.backend.validation.command.infra.openai

import com.docgraph.backend.document.query.application.Block
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Tag("unit")
class ConflictDetectionPromptBuilderTest {

    @Test
    fun `메시지 — system + user 두 개`() {
        val msgs = ConflictDetectionPromptBuilder.build(emptyList(), emptyList(), "criterion")
        assertEquals(2, msgs.size)
        assertEquals("system", msgs[0].role)
        assertEquals("user", msgs[1].role)
    }

    @Test
    fun `system content — 비어있지 않음`() {
        val msgs = ConflictDetectionPromptBuilder.build(emptyList(), emptyList(), "c")
        assertTrue(msgs[0].content.isNotBlank())
    }

    @Test
    fun `user content — 변경 블록 block_id 태깅 포함`() {
        val changed = listOf(block("b1", "변경 내용 X"))
        val user = ConflictDetectionPromptBuilder.build(changed, emptyList(), "c")[1].content
        assertTrue(user.contains("[block_id: b1]"))
        assertTrue(user.contains("변경 내용 X"))
    }

    @Test
    fun `user content — counterpart 블록 block_id 태깅 포함`() {
        val cp = listOf(block("b9", "반대편 본문"))
        val user = ConflictDetectionPromptBuilder.build(emptyList(), cp, "c")[1].content
        assertTrue(user.contains("[block_id: b9]"))
        assertTrue(user.contains("반대편 본문"))
    }

    @Test
    fun `user content — criterion 포함`() {
        val user = ConflictDetectionPromptBuilder.build(emptyList(), emptyList(), "결정사항 반영 여부")[1].content
        assertTrue(user.contains("결정사항 반영 여부"))
    }

    @Test
    fun `빈 changedBlocks — 깨지지 않음`() {
        val msgs = ConflictDetectionPromptBuilder.build(emptyList(), listOf(block("b1", "x")), "c")
        assertEquals(2, msgs.size)
    }

    @Test
    fun `빈 counterpartBlocks — 깨지지 않음`() {
        val msgs = ConflictDetectionPromptBuilder.build(listOf(block("b1", "x")), emptyList(), "c")
        assertEquals(2, msgs.size)
    }

    @Test
    fun `text 내 block_id 토큰 포함 — 그대로 직렬화 (구조 안 깨짐)`() {
        val changed = listOf(block("b1", "본문 [block_id: fake] 인용"))
        val user = ConflictDetectionPromptBuilder.build(changed, emptyList(), "c")[1].content
        assertTrue(user.contains("[block_id: b1]"), "real id 태그 존재")
        assertTrue(user.contains("[block_id: fake]"), "본문 내 fake 토큰도 그대로 포함")
    }

    @Test
    fun `text 내 newline — 단일 라인으로 평탄화`() {
        val changed = listOf(block("b1", "line1\nline2"))
        val user = ConflictDetectionPromptBuilder.build(changed, emptyList(), "c")[1].content
        val pattern = Regex("\\[block_id: b1][^\\n]*line1[^\\n]*line2")
        assertNotNull(pattern.find(user), "b1 한 줄 안에 line1·line2 모두 평탄화되어야 함")
    }

    @Test
    fun `여러 블록 — 각 줄 분리, 순서 보존`() {
        val changed = listOf(block("b1", "x"), block("b2", "y"))
        val user = ConflictDetectionPromptBuilder.build(changed, emptyList(), "c")[1].content
        val b1Idx = user.indexOf("[block_id: b1]")
        val b2Idx = user.indexOf("[block_id: b2]")
        assertTrue(b1Idx in 0 until b2Idx, "b1이 b2보다 앞")
        val between = user.substring(b1Idx + "[block_id: b1]".length, b2Idx)
        assertTrue(between.contains("\n"), "두 블록 사이 줄바꿈")
    }

    private fun block(id: String, text: String) = Block(id, null, "paragraph", text, 0)
}
