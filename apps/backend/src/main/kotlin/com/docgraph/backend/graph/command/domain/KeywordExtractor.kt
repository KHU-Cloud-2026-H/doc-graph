package com.docgraph.backend.graph.command.domain

/**
 * 문서 본문에서 유사도 비교에 쓸 키워드 토큰을 추출한다.
 *
 * 추출 방식(형태소 분석·품사 선별 등)은 구현(infra)이 결정한다. 도메인은 토큰 집합만 본다.
 */
fun interface KeywordExtractor {
    fun extract(text: String?): Set<String>
}
