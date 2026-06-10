package com.docgraph.backend.graph.command.infra

import com.docgraph.backend.graph.command.domain.KeywordExtractor
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.ko.KoreanTokenizer
import org.apache.lucene.analysis.ko.POS
import org.apache.lucene.analysis.ko.tokenattributes.PartOfSpeechAttribute
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.springframework.stereotype.Component

/**
 * Lucene nori 형태소 분석으로 명사·외국어·한자·숫자(NNG·NNP·SL·SH·SN)만 키워드로 추출한다.
 *
 * 조사·어미·기능어는 제외해 같은 단어의 활용형이 같은 토큰으로 모이게 하고, 외래어 대소문자
 * 차이를 없애기 위해 소문자로 정규화한다. Analyzer는 thread-safe(스레드별 컴포넌트 재사용)다.
 */
@Component
class NoriKeywordExtractor : KeywordExtractor {

    private val analyzer: Analyzer = object : Analyzer() {
        override fun createComponents(fieldName: String): TokenStreamComponents =
            TokenStreamComponents(KoreanTokenizer())
    }

    override fun extract(text: String?): Set<String> {
        if (text.isNullOrBlank()) return emptySet()
        val keywords = mutableSetOf<String>()
        analyzer.tokenStream(FIELD, text).use { stream ->
            val term = stream.addAttribute(CharTermAttribute::class.java)
            val pos = stream.addAttribute(PartOfSpeechAttribute::class.java)
            stream.reset()
            while (stream.incrementToken()) {
                if (pos.leftPOS in KEPT_TAGS) {
                    keywords.add(term.toString().lowercase())
                }
            }
            stream.end()
        }
        return keywords
    }

    companion object {
        private const val FIELD = ""
        private val KEPT_TAGS = setOf(
            POS.Tag.NNG, // 일반명사
            POS.Tag.NNP, // 고유명사
            POS.Tag.SL, // 외국어
            POS.Tag.SH, // 한자
            POS.Tag.SN, // 숫자
        )
    }
}
