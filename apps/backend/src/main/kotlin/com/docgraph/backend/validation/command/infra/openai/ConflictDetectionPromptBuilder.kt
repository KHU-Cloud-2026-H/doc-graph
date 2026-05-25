package com.docgraph.backend.validation.command.infra.openai

import com.docgraph.backend.document.query.application.Block

object ConflictDetectionPromptBuilder {

    private const val SYSTEM_PROMPT = """
너는 두 문서 사이의 정합성 충돌을 검출하는 어시스턴트다.
입력으로 (1) source 측 변경 블록, (2) target 측 문서 전체 블록, (3) 검증 기준이 주어진다.
각 블록은 "[block_id: <id>] <text>" 형식으로 라벨링되어 있다. 결과는 반드시 제공된 JSON 스키마에 strict하게 맞춰 응답한다.
충돌이 없으면 conflicts는 빈 배열로 응답한다.
"""

    fun build(
        changedBlocks: List<Block>,
        counterpartBlocks: List<Block>,
        criterion: String,
    ): List<OpenAiChatMessage> = listOf(
        OpenAiChatMessage(OpenAiChatMessage.ROLE_SYSTEM, SYSTEM_PROMPT.trim()),
        OpenAiChatMessage(OpenAiChatMessage.ROLE_USER, userContent(changedBlocks, counterpartBlocks, criterion)),
    )

    private fun userContent(
        changedBlocks: List<Block>,
        counterpartBlocks: List<Block>,
        criterion: String,
    ): String = buildString {
        append("## 검증 기준\n")
        append(criterion)
        append("\n\n## 변경된 블록 (source 측)\n")
        append(serialize(changedBlocks))
        append("\n\n## 반대편 문서 블록 (target 측)\n")
        append(serialize(counterpartBlocks))
    }

    private fun serialize(blocks: List<Block>): String =
        if (blocks.isEmpty()) "(없음)"
        else blocks.joinToString("\n") { "[block_id: ${it.blockId}] ${flatten(it.text ?: "")}" }

    private fun flatten(text: String): String =
        text.replace(Regex("\\s+"), " ").trim()
}
