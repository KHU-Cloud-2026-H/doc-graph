package com.docgraph.backend.graph.command.domain

/**
 * 두 문서 본문(flatText)의 키워드 유사도를 공백 토큰 Jaccard로 계산한다.
 *
 * Notion 링크·멘션으로 엣지가 만들어지지 않은 문서 쌍의 연결 제안(EdgeProposal) 점수에 쓰인다.
 * 0.0(겹치는 토큰 없음) ~ 1.0(토큰 집합 동일).
 */
object KeywordSimilarity {

    /** 이 점수 미만의 문서 쌍은 제안하지 않는다. */
    const val PROPOSAL_SCORE_THRESHOLD = 0.1

    /** source 문서 1건당 생성하는 제안 최대 개수(점수 상위). */
    const val MAX_PROPOSALS_PER_SOURCE = 3

    fun score(source: String?, target: String?): Double {
        val sourceTokens = tokenize(source)
        val targetTokens = tokenize(target)
        if (sourceTokens.isEmpty() || targetTokens.isEmpty()) {
            return 0.0
        }
        val intersection = sourceTokens.intersect(targetTokens).size
        val union = sourceTokens.union(targetTokens).size
        return intersection.toDouble() / union
    }

    private fun tokenize(text: String?): Set<String> =
        text?.lowercase()
            ?.split(WHITESPACE)
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()

    private val WHITESPACE = Regex("\\s+")
}