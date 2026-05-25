package com.docgraph.backend.validation.command.infra.openai

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OpenAiChatCompletionRequest(
    val model: String,
    val messages: List<OpenAiChatMessage>,
    @JsonProperty("response_format") val responseFormat: Map<String, Any>?,
)
