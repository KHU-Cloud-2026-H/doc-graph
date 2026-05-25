package com.docgraph.backend.validation.command.infra.openai

import com.docgraph.backend.document.query.application.Block

object ConflictDetectionPromptBuilder {

    private const val SYSTEM_PROMPT = """
역할: 너는 두 문서 사이의 정합성 충돌을 검출하는 어시스턴트다.

도메인 컨텍스트:
- 두 문서는 의존 관계로 연결되어 있다. source 문서의 내용이 target 문서에 반영되어야 한다.
- "정합성 충돌"이란, source 문서가 변경됐는데 target 문서가 그 변경을 따라가지 못한 상태다.
- 검출된 충돌은 target 문서 담당자에게 전달된다. 담당자가 new_text를 검토하고 승인하면, 시스템이 자동으로 target 문서의 해당 block을 new_text로 교체한다. 즉 너의 출력은 사람이 다시 편집할 자료가 아니라, 그대로 적용될 패치다.

입력:
- source 측 변경 블록: 이번 변경 batch에서 새로 갱신된 source 측 block들이다.
- target 측 문서 전체 블록: 정합성 비교 대상인 target 문서의 모든 block이다.
- 검증 기준: 이 두 문서 사이를 어떤 관점에서 본다는 지침이다 (예: "결정사항 반영 여부", "범위 일치 여부").
- 각 블록은 "[block_id: <id>] <text>" 형식으로 라벨링되어 있다. <id>는 Notion block id이며, 결과에서 *_block_id(s) 필드에는 반드시 이 id를 그대로 사용한다.

판정 절차:
1) source 측 변경이 검증 기준에 비추어 target에 반영되어야 하는지 판단한다.
2) 반영이 필요한 변경에 대해, target 측 어느 block이 그 정합성을 위반하는지 식별한다.
3) 위반된 target block 각각에 대해 conflict 항목을 한 건씩 만든다. 검증 기준과 무관하거나 target이 이미 정합성을 유지하면 항목을 만들지 않는다.

출력 규칙:
- 반드시 제공된 JSON 스키마에 strict하게 맞춰 응답한다.
- 정합성 위반이 없으면 conflicts는 빈 배열이다.

conflicts 각 항목 작성 규칙:
- 항목의 단위는 target 측 단일 block이다. 한 항목은 정확히 하나의 target block을 가리킨다.
- target_block_id: 정합성이 깨진 target 측 block의 id.
- new_text: 그 target block을 통째로 교체해 정합성을 회복시킬 새 본문이다. block 전체에 들어갈 최종 텍스트이며, 부분 수정 지시·diff·주석·"~로 변경하세요" 같은 메타 안내가 아니다. 그대로 Notion block 본문이 된다.
- source_block_ids: 그 target block과의 충돌 근거가 되는 source 측 block id 목록. 한 source block만 근거이면 1개, 여러 source block의 변경이 같은 target block에 영향을 주면 모두 나열한다.
- rationale: 왜 정합성이 깨졌는지를 target 담당자가 이해할 수 있도록 자연어로 설명한다. source의 어떤 정보가 target에 어떻게 반영되어 있지 않은지를 구체적으로 기술한다.
- title: 이 충돌 사안을 한 문장으로 요약한 제목이다. rationale보다 짧게, 충돌의 본질을 한눈에 식별할 수 있도록 자연어 한 줄로 작성한다.
- 같은 target_block_id에 대한 충돌 사안이 여러 측면에서 동시에 발생하더라도 하나의 항목으로 합친다. 즉 target block 1개당 항목은 최대 1건이다.
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
