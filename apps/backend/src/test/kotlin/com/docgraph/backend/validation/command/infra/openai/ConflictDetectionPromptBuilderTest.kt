package com.docgraph.backend.validation.command.infra.openai

import com.docgraph.backend.document.query.application.Block
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Tag("unit")
class ConflictDetectionPromptBuilderTest {

    @Test
    fun `메시지 — system + user 두 개`() {
        val msgs = ConflictDetectionPromptBuilder.buildFirstValidation(emptyList(), emptyList(), "criterion")
        assertEquals(2, msgs.size)
        assertEquals("system", msgs[0].role)
        assertEquals("user", msgs[1].role)
    }

    @Test
    fun `system content — 비어있지 않음`() {
        val msgs = ConflictDetectionPromptBuilder.buildFirstValidation(emptyList(), emptyList(), "c")
        assertTrue(msgs[0].content!!.isNotBlank())
    }

    @Test
    fun `system content — finding 단위·필드 명세 안내`() {
        val system = ConflictDetectionPromptBuilder.buildFirstValidation(emptyList(), emptyList(), "c")[0].content!!
        assertTrue(system.contains("target_block_id"), "target_block_id 필드명 안내")
        assertTrue(system.contains("new_text"), "new_text 필드명 안내")
        assertTrue(system.contains("source_block_ids"), "source_block_ids 필드명 안내")
    }

    @Test
    fun `system content — title 한 줄 요약 작성 지시`() {
        val system = ConflictDetectionPromptBuilder.buildFirstValidation(emptyList(), emptyList(), "c")[0].content!!
        assertTrue(system.contains("title"), "title 필드 작성 안내")
    }

    @Test
    fun `system content — 위반된 target block 각각에 대해 항목 생성 안내`() {
        val system = ConflictDetectionPromptBuilder.buildFirstValidation(emptyList(), emptyList(), "c")[0].content!!
        assertTrue(system.contains("target_conflict_block"), "target_conflict_block 필드 안내")
        assertTrue(system.contains("각각"), "위반된 target block 각각에 대해 conflict 항목 생성 안내")
    }

    @Test
    fun `user content — 변경 블록 block_id 태깅 포함`() {
        val changed = listOf(block("b1", "변경 내용 X"))
        val user = ConflictDetectionPromptBuilder.buildFirstValidation(changed, emptyList(), "c")[1].content!!
        assertTrue(user.contains("[block_id: b1]"))
        assertTrue(user.contains("변경 내용 X"))
    }

    @Test
    fun `user content — counterpart 블록 block_id 태깅 포함`() {
        val cp = listOf(block("b9", "반대편 본문"))
        val user = ConflictDetectionPromptBuilder.buildFirstValidation(emptyList(), cp, "c")[1].content!!
        assertTrue(user.contains("[block_id: b9]"))
        assertTrue(user.contains("반대편 본문"))
    }

    @Test
    fun `user content — criterion 포함`() {
        val user = ConflictDetectionPromptBuilder.buildFirstValidation(emptyList(), emptyList(), "결정사항 반영 여부")[1].content!!
        assertTrue(user.contains("결정사항 반영 여부"))
    }

    @Test
    fun `빈 changedBlocks — 깨지지 않음`() {
        val msgs = ConflictDetectionPromptBuilder.buildFirstValidation(emptyList(), listOf(block("b1", "x")), "c")
        assertEquals(2, msgs.size)
    }

    @Test
    fun `빈 counterpartBlocks — 깨지지 않음`() {
        val msgs = ConflictDetectionPromptBuilder.buildFirstValidation(listOf(block("b1", "x")), emptyList(), "c")
        assertEquals(2, msgs.size)
    }

    @Test
    fun `text 내 block_id 토큰 포함 — 그대로 직렬화 (구조 안 깨짐)`() {
        val changed = listOf(block("b1", "본문 [block_id: fake] 인용"))
        val user = ConflictDetectionPromptBuilder.buildFirstValidation(changed, emptyList(), "c")[1].content!!
        assertTrue(user.contains("[block_id: b1]"), "real id 태그 존재")
        assertTrue(user.contains("[block_id: fake]"), "본문 내 fake 토큰도 그대로 포함")
    }

    @Test
    fun `text 내 newline — 단일 라인으로 평탄화`() {
        val changed = listOf(block("b1", "line1\nline2"))
        val user = ConflictDetectionPromptBuilder.buildFirstValidation(changed, emptyList(), "c")[1].content!!
        val pattern = Regex("\\[block_id: b1][^\\n]*line1[^\\n]*line2")
        assertNotNull(pattern.find(user), "b1 한 줄 안에 line1·line2 모두 평탄화되어야 함")
    }

    @Test
    fun `여러 블록 — 각 줄 분리, 순서 보존`() {
        val changed = listOf(block("b1", "x"), block("b2", "y"))
        val user = ConflictDetectionPromptBuilder.buildFirstValidation(changed, emptyList(), "c")[1].content!!
        val b1Idx = user.indexOf("[block_id: b1]")
        val b2Idx = user.indexOf("[block_id: b2]")
        assertTrue(b1Idx in 0 until b2Idx, "b1이 b2보다 앞")
        val between = user.substring(b1Idx + "[block_id: b1]".length, b2Idx)
        assertTrue(between.contains("\n"), "두 블록 사이 줄바꿈")
    }

    @Test
    fun `buildRevalidation — 변경 전·후 섹션 구분 + target`() {
        val msgs = ConflictDetectionPromptBuilder.buildRevalidation(
            beforeBlocks = listOf(block("b1", "옛 내용")),
            afterBlocks = listOf(block("b1", "새 내용")),
            targetBlocks = listOf(block("t1", "대상")),
            criterion = "결정사항 반영 여부",
        )
        assertEquals(2, msgs.size)
        assertTrue(msgs[0].content!!.contains("target_block_id"), "출력 규칙 공유")
        val user = msgs[1].content!!
        assertTrue(user.contains("옛 내용"), "변경 전 본문 포함")
        assertTrue(user.contains("새 내용"), "변경 후 본문 포함")
        assertTrue(user.contains("[block_id: t1]") && user.contains("대상"))
        assertTrue(user.contains("변경 전") && user.contains("변경 후"), "전·후 섹션 구분")
    }

    @Test
    fun `buildFirstValidation — 블록 라인 단일상태, before·after 라벨 없음`() {
        val user = ConflictDetectionPromptBuilder.buildFirstValidation(
            listOf(block("b1", "본문 X")), listOf(block("t1", "대상")), "c",
        )[1].content!!
        assertTrue(user.contains("[block_id: b1] 본문 X"), "source 단일상태 라인")
        assertTrue(user.contains("[block_id: t1] 대상"), "target 단일상태 라인")
        assertFalse(user.contains("before:"), "before: 라벨 없음")
        assertFalse(user.contains("after:"), "after: 라벨 없음")
    }

    @Test
    fun `buildRevalidation — 블록 라인 단일상태, diff는 섹션이 표현`() {
        val user = ConflictDetectionPromptBuilder.buildRevalidation(
            beforeBlocks = listOf(block("b1", "옛 내용")),
            afterBlocks = listOf(block("b1", "새 내용")),
            targetBlocks = listOf(block("t1", "대상")),
            criterion = "c",
        )[1].content!!
        assertTrue(user.contains("[block_id: b1] 옛 내용"), "변경 전 단일상태 라인")
        assertTrue(user.contains("[block_id: b1] 새 내용"), "변경 후 단일상태 라인")
        assertFalse(user.contains("before:"), "라인 단위 before: 라벨 없음")
        assertFalse(user.contains("after:"), "라인 단위 after: 라벨 없음")
    }

    private fun block(id: String, text: String) = Block(id, null, "paragraph", text, null, 0)
}
