package com.docgraph.backend.graph.command.domain

import org.springframework.stereotype.Component

/**
 * 두 문서 본문에서 추출한 키워드 집합의 Jaccard 유사도를 계산한다.
 *
 * 키워드 추출은 [KeywordExtractor]에 위임한다(형태소 분석으로 명사·외래어·숫자 등 선별).
 * Notion 링크·멘션으로 엣지가 만들어지지 않은 문서 쌍의 연결 제안(EdgeProposal) 점수에 쓰인다.
 * 0.0(겹치는 키워드 없음) ~ 1.0(키워드 집합 동일).
 */
@Component
class KeywordSimilarity(
    private val keywordExtractor: KeywordExtractor,
) {

    fun score(source: String?, target: String?): Double {
        val sourceKeywords = keywordExtractor.extract(source)
        val targetKeywords = keywordExtractor.extract(target)
        if (sourceKeywords.isEmpty() || targetKeywords.isEmpty()) {
            return 0.0
        }
        val intersection = sourceKeywords.intersect(targetKeywords).size
        val union = sourceKeywords.union(targetKeywords).size
        return intersection.toDouble() / union
    }

    companion object {
        /** 이 점수 미만의 문서 쌍은 제안하지 않는다. */
        const val PROPOSAL_SCORE_THRESHOLD = 0.1

        /** source 문서 1건당 생성하는 제안 최대 개수(점수 상위). */
        const val MAX_PROPOSALS_PER_SOURCE = 3
    }
}
