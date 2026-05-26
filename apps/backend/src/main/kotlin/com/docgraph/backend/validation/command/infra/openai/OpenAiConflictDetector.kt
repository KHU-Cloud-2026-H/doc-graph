package com.docgraph.backend.validation.command.infra.openai

import com.docgraph.backend.document.query.application.Block
import com.docgraph.backend.validation.command.domain.ConflictDetector
import com.docgraph.backend.validation.command.domain.DetectedConflict
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Component
class OpenAiConflictDetector(
    private val restClient: RestClient,
    private val parser: ConflictDetectionResponseParser,
    private val props: OpenAiProperties,
) : ConflictDetector {

    override fun detect(
        changedBlocks: List<Block>,
        counterpartBlocks: List<Block>,
        criterion: String,
    ): List<DetectedConflict> {
        val request = OpenAiChatCompletionRequest(
            model = props.model,
            messages = ConflictDetectionPromptBuilder.build(changedBlocks, counterpartBlocks, criterion),
            responseFormat = ConflictDetectionResponseSchema.responseFormat(),
        )
        val response = restClient.post()
            .uri("/v1/chat/completions")
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
