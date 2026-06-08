package com.docgraph.backend.validation.command.infra.openai

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenAiChatCompletionResponse(
    val choices: List<Choice>,
    // 정상 응답엔 항상 존재하나, 빈 choices 등 비정상 본문에서 결측될 수 있어 nullable.
    val model: String? = null,
    val usage: Usage? = null,
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Choice(
        val message: OpenAiChatMessage,
        // content가 비는 원인 진단용: length(토큰 소진)·content_filter·stop 등.
        @JsonProperty("finish_reason") val finishReason: String? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Usage(
        @JsonProperty("prompt_tokens") val promptTokens: Int,
        @JsonProperty("completion_tokens") val completionTokens: Int,
        @JsonProperty("total_tokens") val totalTokens: Int,
    )
}
