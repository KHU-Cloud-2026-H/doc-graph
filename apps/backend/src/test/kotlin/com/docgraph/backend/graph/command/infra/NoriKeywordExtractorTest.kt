package com.docgraph.backend.graph.command.infra

import com.docgraph.backend.graph.command.domain.KeywordSimilarity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("unit")
class NoriKeywordExtractorTest {

    private val extractor = NoriKeywordExtractor()

    @Test
    fun `조사를 제거하고 명사 원형만 추출`() {
        val result = extractor.extract("결제를 검증한다")
        assertTrue(result.contains("결제"))
        assertTrue(result.contains("검증"))
        assertFalse(result.contains("결제를"))
    }

    @Test
    fun `조사 변형은 같은 키워드로 추출`() {
        assertEquals(extractor.extract("결제를"), extractor.extract("결제는"))
    }

    @Test
    fun `외래어는 소문자 키워드로 추출`() {
        val result = extractor.extract("API 설계")
        assertTrue(result.contains("api"))
        assertTrue(result.contains("설계"))
    }

    @Test
    fun `숫자는 키워드로 추출`() {
        assertTrue(extractor.extract("버전 2").contains("2"))
    }

    @Test
    fun `명사 외래어 숫자가 아닌 기능어는 제외`() {
        assertTrue(extractor.extract("그리고 또한").isEmpty())
    }

    @Test
    fun `null과 공백은 빈 집합`() {
        assertTrue(extractor.extract(null).isEmpty())
        assertTrue(extractor.extract("   ").isEmpty())
    }

    @Test
    fun `조사만 다른 같은 키워드 문서는 유사도가 threshold를 넘는다`() {
        // 공백 분리였다면 "결제를"≠"결제는"이라 공통 토큰이 없어 유사도 0.
        // 형태소 추출은 "결제"를 공통 키워드로 잡아 제안 threshold를 넘긴다.
        val similarity = KeywordSimilarity(extractor)
        val score = similarity.score("결제를 진행", "결제는 완료")
        assertTrue(score >= KeywordSimilarity.PROPOSAL_SCORE_THRESHOLD)
    }
}
