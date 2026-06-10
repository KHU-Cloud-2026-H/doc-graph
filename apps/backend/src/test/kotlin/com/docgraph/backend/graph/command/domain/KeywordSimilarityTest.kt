package com.docgraph.backend.graph.command.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("unit")
class KeywordSimilarityTest {

    // KeywordSimilarity는 Jaccard 계산만 담당 — 추출은 공백 토큰 fake로 고정한다.
    // 실제 형태소 추출은 NoriKeywordExtractorTest가 검증한다.
    private val keywordSimilarity = KeywordSimilarity { text ->
        text?.split(" ")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
    }

    @Test
    fun `동일 키워드 집합은 유사도 1`() {
        assertEquals(1.0, keywordSimilarity.score("기획 범위 일정", "기획 범위 일정"))
    }

    @Test
    fun `공통 키워드 없으면 유사도 0`() {
        assertEquals(0.0, keywordSimilarity.score("기획 범위", "디자인 색상"))
    }

    @Test
    fun `부분 겹침은 키워드 Jaccard`() {
        // {a,b,c} vs {b,c,d} → 교집합 2 / 합집합 4 = 0.5
        assertEquals(0.5, keywordSimilarity.score("a b c", "b c d"))
    }

    @Test
    fun `중복 키워드는 집합으로 취급`() {
        // {a,b} vs {a,b} → 1.0
        assertEquals(1.0, keywordSimilarity.score("a a b", "a b b"))
    }

    @Test
    fun `한쪽이라도 키워드가 없으면 유사도 0`() {
        assertEquals(0.0, keywordSimilarity.score(null, "기획 범위"))
        assertEquals(0.0, keywordSimilarity.score("기획 범위", null))
        assertEquals(0.0, keywordSimilarity.score("", "기획 범위"))
    }
}
