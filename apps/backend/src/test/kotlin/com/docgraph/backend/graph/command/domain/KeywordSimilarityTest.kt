package com.docgraph.backend.graph.command.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("unit")
class KeywordSimilarityTest {

    @Test
    fun `동일 텍스트는 유사도 1`() {
        assertEquals(1.0, KeywordSimilarity.score("기획 범위 일정", "기획 범위 일정"))
    }

    @Test
    fun `공통 토큰 없으면 유사도 0`() {
        assertEquals(0.0, KeywordSimilarity.score("기획 범위", "디자인 색상"))
    }

    @Test
    fun `부분 겹침은 토큰 Jaccard`() {
        // 토큰 {a,b,c} vs {b,c,d} → 교집합 2 / 합집합 4 = 0.5
        assertEquals(0.5, KeywordSimilarity.score("a b c", "b c d"))
    }

    @Test
    fun `대소문자 무시·중복 토큰은 집합으로 취급`() {
        // {scope, range} vs {scope, range} → 1.0
        assertEquals(1.0, KeywordSimilarity.score("Scope scope RANGE", "scope range range"))
    }

    @Test
    fun `null 또는 공백 텍스트는 유사도 0`() {
        assertEquals(0.0, KeywordSimilarity.score(null, "기획 범위"))
        assertEquals(0.0, KeywordSimilarity.score("기획 범위", null))
        assertEquals(0.0, KeywordSimilarity.score("   ", "기획 범위"))
    }
}