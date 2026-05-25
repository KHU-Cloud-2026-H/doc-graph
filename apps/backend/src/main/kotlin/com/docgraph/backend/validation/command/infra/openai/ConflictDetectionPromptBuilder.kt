package com.docgraph.backend.validation.command.infra.openai

import com.docgraph.backend.document.query.application.Block

object ConflictDetectionPromptBuilder {

    private const val ROLE = "역할: 너는 두 문서 사이의 정합성 충돌을 검출하는 어시스턴트다."

    private const val OUTPUT_RULES = """
- 각 블록은 "[block_id: <id>] <text>" 형식으로 라벨링되어 있다. <id>는 Notion block id이며, 결과에서 *_block_id(s) 필드에는 반드시 이 id를 그대로 사용한다.

출력 규칙:
- 반드시 제공된 JSON 스키마에 strict하게 맞춰 응답한다.
- 정합성 위반이 없으면 conflicts는 빈 배열이다.

conflicts 각 항목 작성 규칙:
- 항목의 단위는 target 측 단일 block이다. 한 항목은 정확히 하나의 target block을 가리킨다.
- target_block_id: 정합성이 깨진 target 측 block의 id.
- new_text: 그 target block을 통째로 교체해 정합성을 회복시킬 새 본문이다. block 전체에 들어갈 최종 텍스트이며, 부분 수정 지시·diff·주석·"~로 변경하세요" 같은 메타 안내가 아니다. 그대로 Notion block 본문이 된다.
- source_block_ids: 그 target block과의 충돌 근거가 되는 source 측 block id 목록. 한 source block만 근거이면 1개, 여러 source block이 같은 target block에 영향을 주면 모두 나열한다.
- rationale: 왜 정합성이 깨졌는지를 target 담당자가 이해할 수 있도록 자연어로 설명한다.
- title: 이 충돌 사안을 한 문장으로 요약한 제목이다. rationale보다 짧게, 충돌의 본질을 한눈에 식별할 수 있도록 자연어 한 줄로 작성한다.
- 같은 target_block_id에 대한 충돌 사안이 여러 측면에서 동시에 발생하더라도 하나의 항목으로 합친다. 즉 target block 1개당 항목은 최대 1건이다.
"""

    private const val FIRST_VALIDATION_INTRO = """
도메인 컨텍스트:
- 두 문서는 의존 관계로 연결되어 있다. source 문서의 내용이 target 문서에 반영되어야 한다.
- 검출된 충돌은 target 문서 담당자에게 전달된다. 담당자가 new_text를 승인하면 시스템이 target 문서의 해당 block을 new_text로 교체한다. 즉 출력은 그대로 적용될 패치다.

입력:
- source 측 전체 블록: source 문서의 모든 block이다.
- target 측 전체 블록: 정합성 비교 대상인 target 문서의 모든 block이다.
- 검증 기준: 이 두 문서를 어떤 관점에서 본다는 지침이다.

판정 절차:
1) source 내용이 검증 기준에 비추어 target에 반영되어야 하는지 판단한다.
2) 반영이 필요한데 위반된 target block 각각에 대해 conflict 항목을 만든다. target이 이미 정합하면 만들지 않는다.
"""

    private const val REVALIDATION_INTRO = """
도메인 컨텍스트:
- 두 문서는 의존 관계로 연결되어 있다. source 문서의 내용이 target 문서에 반영되어야 한다.
- "정합성 충돌"이란 source 문서가 변경됐는데 target 문서가 그 변경을 따라가지 못한 상태다.
- 검출된 충돌은 target 문서 담당자에게 전달된다. 담당자가 new_text를 승인하면 시스템이 target 문서의 해당 block을 new_text로 교체한다. 즉 출력은 그대로 적용될 패치다.

입력:
- source 측 변경 전 블록 / 변경 후 블록: source 문서의 직전 상태와 현재 상태다. 둘을 비교해 무엇이 어떻게 바뀌었는지 파악한다.
- target 측 전체 블록: 정합성 비교 대상인 target 문서의 모든 block이다.
- 검증 기준: 이 두 문서를 어떤 관점에서 본다는 지침이다.

판정 절차:
1) 변경 전→후 차이가 검증 기준에 비추어 target에 반영되어야 하는 변화인지 판단한다.
2) 그 변화가 반영되지 않은 target block 각각에 대해 conflict 항목을 만든다. 이미 반영됐으면 만들지 않는다.
"""

    fun buildFirstValidation(
        sourceBlocks: List<Block>,
        targetBlocks: List<Block>,
        criterion: String,
    ): List<OpenAiChatMessage> = listOf(
        OpenAiChatMessage(OpenAiChatMessage.ROLE_SYSTEM, systemContent(FIRST_VALIDATION_INTRO)),
        OpenAiChatMessage(
            OpenAiChatMessage.ROLE_USER,
            buildString {
                append("## 검증 기준\n").append(criterion)
                append("\n\n## source 측 전체 블록\n").append(serialize(sourceBlocks))
                append("\n\n## target 측 전체 블록\n").append(serialize(targetBlocks))
            },
        ),
    )

    fun buildRevalidation(
        beforeBlocks: List<Block>,
        afterBlocks: List<Block>,
        targetBlocks: List<Block>,
        criterion: String,
    ): List<OpenAiChatMessage> = listOf(
        OpenAiChatMessage(OpenAiChatMessage.ROLE_SYSTEM, systemContent(REVALIDATION_INTRO)),
        OpenAiChatMessage(
            OpenAiChatMessage.ROLE_USER,
            buildString {
                append("## 검증 기준\n").append(criterion)
                append("\n\n## source 측 변경 전 블록\n").append(serialize(beforeBlocks))
                append("\n\n## source 측 변경 후 블록\n").append(serialize(afterBlocks))
                append("\n\n## target 측 전체 블록\n").append(serialize(targetBlocks))
            },
        ),
    )

    private fun systemContent(intro: String): String =
        (ROLE + "\n\n" + intro.trim() + "\n\n" + OUTPUT_RULES.trim()).trim()

    private fun serialize(blocks: List<Block>): String =
        if (blocks.isEmpty()) "(없음)"
        else blocks.joinToString("\n") { "[block_id: ${it.blockId}] ${flatten(it.text ?: "")}" }

    private fun flatten(text: String): String =
        text.replace(Regex("\\s+"), " ").trim()
}
