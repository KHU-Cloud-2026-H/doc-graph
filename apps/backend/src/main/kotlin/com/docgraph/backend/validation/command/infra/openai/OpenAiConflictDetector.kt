package com.docgraph.backend.validation.command.infra.openai

import com.docgraph.backend.validation.command.domain.ConflictDetectionInput
import com.docgraph.backend.validation.command.domain.ConflictDetector
import com.docgraph.backend.validation.command.domain.DetectedConflict
import com.docgraph.backend.validation.command.domain.FirstValidationInput
import com.docgraph.backend.validation.command.domain.RevalidationInput
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

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
        val response = restClient.post()
            .uri { it.path("/chat/completions").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body<OpenAiChatCompletionResponse>()
            ?: error("OpenAI response body is null")
        val content = response.choices.firstOrNull()?.message?.content
            ?: error("OpenAI response has no choices")
        return parser.parse(content)
    }
}
