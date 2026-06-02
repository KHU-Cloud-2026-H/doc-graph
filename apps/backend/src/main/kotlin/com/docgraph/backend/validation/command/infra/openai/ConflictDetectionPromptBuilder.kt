package com.docgraph.backend.validation.command.infra.openai

import com.docgraph.backend.document.query.application.Block

object ConflictDetectionPromptBuilder {

    private const val ROLE = """
당신은 소프트웨어 프로젝트 문서 정합성 검증 전문가이다.

당신의 역할은 두 개의 프로젝트 문서를 비교하여
논리적, 의미적, 정책적, 데이터적, 워크플로우적 충돌을 탐지하는 것이다.

다음과 같은 충돌을 탐지해야 한다:

- 요구사항 충돌
- 회의 결정사항 전파 누락
- API/DB 스키마 불일치
- 데이터 타입 불일치
- 네이밍 컨벤션 불일치
- 상태 머신 및 워크플로우 충돌
- 권한(RBAC) 및 보안 정책 충돌
- 프론트엔드/백엔드 데이터 불일치
- 비즈니스 정책 충돌
- 검증 규칙 충돌
- 제거된 기능이 여전히 참조되는 문제
- 의미적으로 상충되는 구현 가정

단순 키워드 매칭이 아니라,
문맥과 의미 기반으로 충돌 여부를 판단해야 한다.

표현 방식이 달라도 실제 의미가 충돌하면 탐지해야 한다.
"""

    private const val OUTPUT_RULES = """

-----------------------------------
출력 규칙
-----------------------------------

- 각 블록은 "[block_id: <id>] <text>" 형식으로 라벨링되어 있다. <id>는 Notion block id이며, 결과에서 *_block_id(s) 필드에는 반드시 이 id를 그대로 사용한다.


반드시 JSON 형식만 출력하라.
설명 문장이나 Markdown은 절대 출력하지 마라.

출력 형식:


{
  "summary": {
    "total_inconsistencies": <숫자>,
    "critical_count": <숫자>,
    "warning_count": <숫자>
  },
  "inconsistencies": [
    {
      "severity": "치명적 | 경고",

      "category": "정책 충돌 | 스키마 불일치 | 워크플로우 충돌 | 네이밍 불일치 | 보안 충돌 | 데이터 타입 충돌 | 요구사항 드리프트 | 제거된 기능 참조 | 기타",

      "title": "<짧은 충돌 제목>",

      "source_evidence": {
        "block_id": "<source block_id>",
        "reference": "<가능하면 섹션명 또는 제목>",
        "text": "<관련 원문 발췌>"
      },

      "target_conflict_block": {
        "block_id": "<정합성이 깨진 target block_id>",
        "reference": "<가능하면 섹션명 또는 제목>",
        "text": "<충돌이 발생한 target 원문 발췌>"
      },

      "related_blocks": {
        "source_block_ids": ["<source block_id>"],
        "target_block_ids": ["<target block_id>"]
      },

      "reason": "<왜 충돌하는지에 대한 상세 설명>",

      "impact": "<실제 구현/운영/보안/비즈니스 측면에서 발생 가능한 영향>",

      "new_text": "<target block을 그대로 대체할 최종 수정 텍스트>"
      }
    }
  ]
}

-----------------------------------
중요 규칙
-----------------------------------

1. 표현 방식이 달라도 의미가 충돌하면 탐지하라.

2. 근거 없는 충돌은 생성하지 마라.

3. 실제 충돌이 없는 경우 억지로 만들어내지 마라.

4. Recall보다 Precision을 우선하라.

5. 단순 문체 차이는 무시하라.
단, 구현 혼란을 유발하는 경우는 예외이다.

6. 구현 레벨의 정합성을 우선적으로 검증하라.

7. 다음 문서들은 모두 동등하게 중요한 기준 문서로 간주한다:
- 회의록
- PRD
- API 명세
- DB 스키마
- QA 문서
- 프론트엔드 명세
- 아키텍처 문서

단, 특정 문서에서 정책 변경/삭제가 명시된 경우에는 최신 결정사항으로 우선 취급한다.

8. 기능 삭제/변경이 선언되었는데 다른 문서가 여전히 이를 참조한다면:
- "요구사항 드리프트"
또는
- "제거된 기능 참조"
로 분류하라.

9. 충돌이 없는 경우 반드시 아래 형식만 반환하라:

{
    "summary": {
    "total_inconsistencies": 0,
    "critical_count": 0,
    "warning_count": 0
    },
    "inconsistencies": []
}
"""

    private const val FIRST_VALIDATION_INTRO = """
도메인 컨텍스트:
- 두 문서는 의존 관계로 연결되어 있다. source 문서의 내용이 target 문서에 반영되어야 한다.
- 검출된 충돌은 target 문서 담당자에게 전달된다. 

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
- 검출된 충돌은 target 문서 담당자에게 전달된다. 

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

    private fun systemContent(): String =
        (ROLE + "\n\n" + OUTPUT_RULES.trim()).trim()

    private fun serialize(blocks: List<Block>): String =
        if (blocks.isEmpty()) "(없음)"
        else blocks.joinToString("\n") { "[block_id: ${it.blockId}] ${flatten(it.text ?: "")}" }

    private fun flatten(text: String): String =
        text.replace(Regex("\\s+"), " ").trim()
}
