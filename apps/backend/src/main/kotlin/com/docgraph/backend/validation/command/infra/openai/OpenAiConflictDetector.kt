package com.docgraph.backend.validation.command.infra.openai

import com.docgraph.backend.validation.command.domain.ConflictDetectionInput
import com.docgraph.backend.validation.command.domain.ConflictDetectionResponseException
import com.docgraph.backend.validation.command.domain.ConflictDetector
import com.docgraph.backend.validation.command.domain.DetectedConflict
import com.docgraph.backend.validation.command.domain.FirstValidationInput
import com.docgraph.backend.validation.command.domain.RevalidationInput
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import tools.jackson.core.JacksonException

@Component
class OpenAiConflictDetector(
    @Qualifier("openAiRestClient")
    private val restClient: RestClient,
    private val parser: ConflictDetectionResponseParser,
    private val props: OpenAiProperties,
) : ConflictDetector {

    override fun detect(input: ConflictDetectionInput): List<DetectedConflict> {
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
        val response = restClient.post()
            .uri { it.path("/chat/completions").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body<OpenAiChatCompletionResponse>()
            ?: throw ConflictDetectionResponseException("OpenAI 응답 본문이 비어 있음")
        val content = response.choices.firstOrNull()?.message?.content
            ?: throw ConflictDetectionResponseException("OpenAI 응답에 choices가 없음")
        return try {
            parser.parse(content)
        } catch (e: JacksonException) {
            throw ConflictDetectionResponseException("OpenAI 응답 스키마 불일치 — 파싱 실패", e)
        }
    }
}
