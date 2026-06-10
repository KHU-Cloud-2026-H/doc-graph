package com.docgraph.backend.validation.command.infra.openai

import com.docgraph.backend.validation.command.domain.AiUsage
import com.docgraph.backend.validation.command.domain.ConflictDetectionInput
import com.docgraph.backend.validation.command.domain.ConflictDetectionResponseException
import com.docgraph.backend.validation.command.domain.ConflictDetectionResult
import com.docgraph.backend.validation.command.domain.ConflictDetector
import com.docgraph.backend.validation.command.domain.FirstValidationInput
import com.docgraph.backend.validation.command.domain.RevalidationInput
import com.docgraph.backend.config.ConditionalOnRealHttpAdapter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.body
import tools.jackson.core.JacksonException
import java.io.IOException

@Component
@ConditionalOnRealHttpAdapter
class OpenAiConflictDetector(
    @Qualifier("openAiRestClient")
    private val restClient: RestClient,
    private val parser: ConflictDetectionResponseParser,
    private val props: OpenAiProperties,
) : ConflictDetector {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun detect(input: ConflictDetectionInput): ConflictDetectionResult {
        val messages = when (input) {
            is FirstValidationInput -> ConflictDetectionPromptBuilder.buildFirstValidation(
                input.sourceBlocks, input.targetBlocks, input.criterion,
            )
            is RevalidationInput -> ConflictDetectionPromptBuilder.buildRevalidation(
                input.sourceBeforeBlocks, input.sourceAfterBlocks, input.targetBlocks, input.criterion,
            )
        }
        val request = OpenAiChatCompletionRequest(
            model = props.model,
            messages = messages,
            responseFormat = ConflictDetectionResponseSchema.responseFormat(),
        )
        // 전송 오류(4xx/5xx/timeout)는 RestClient가 던지는 예외 그대로 전파하고,
        // 200 응답이지만 결과를 쓸 수 없는 경우만 ConflictDetectionResponseException으로 환원한다.
        val response = try {
            restClient.post()
                .uri { it.path("/chat/completions").build() }
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body<OpenAiChatCompletionResponse>()
                ?: throw ConflictDetectionResponseException("OpenAI 응답 본문이 비어 있음")
        } catch (e: RestClientException) {
            throw translateExtractionFailure(e)
        }
        val choice = response.choices.firstOrNull()
            ?: throw ConflictDetectionResponseException("OpenAI 응답에 choices가 없음")
        val content = choice.message.content
            ?: throw ConflictDetectionResponseException(
                "OpenAI 응답 content가 비어 있음 — finish_reason=${choice.finishReason}, refusal=${choice.message.refusal}",
            )
        val conflicts = try {
            parser.parse(content)
        } catch (e: JacksonException) {
            throw ConflictDetectionResponseException("OpenAI 응답 스키마 불일치 — 파싱 실패", e)
        }
        val usage = response.usage?.let { u ->
            response.model?.let { m -> AiUsage(m, u.promptTokens, u.completionTokens, u.totalTokens) }
        }
        return ConflictDetectionResult(conflicts, usage)
    }

    /**
     * `.body()` 추출 단계에서 던져진 [RestClientException]을 운영 분류·재시도 정책에 맞게 재환원한다.
     *
     * - 상태코드 예외(4xx/5xx)·전송 예외(연결 타임아웃 등)는 이미 분류·재시도 대상이므로 그대로 전파.
     * - 그 외 일반 [RestClientException]은 본문 수신/추출 중 끊긴 경우다. RestClient가 이를
     *   [ResourceAccessException]이 아닌 평범한 예외로 환원해 재시도·TIMEOUT 분류 경로를 우회하므로,
     *   근본 원인이 I/O면(읽기 타임아웃·연결 끊김) 전송 실패([ResourceAccessException], 재시도 대상)로,
     *   아니면(본문을 JSON으로 파싱 불가) 응답 오류([ConflictDetectionResponseException], terminal)로 되돌린다.
     */
    private fun translateExtractionFailure(e: RestClientException): RuntimeException {
        if (e is RestClientResponseException || e is ResourceAccessException) return e
        val root = e.rootCause
        return if (root is IOException) {
            log.warn("OpenAI 응답 본문 수신 중단 — 전송 실패로 재시도: {}", root.message)
            ResourceAccessException("OpenAI 응답 본문 수신 중단: ${root.message}", root)
        } else {
            ConflictDetectionResponseException("OpenAI 응답 본문 파싱 실패 — 응답 형식 불일치", e)
        }
    }
}
