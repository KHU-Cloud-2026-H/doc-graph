package com.docgraph.backend.validation.command.infra.openai

import com.docgraph.backend.document.query.application.Block

object ConflictDetectionPromptBuilder {

    private const val SYSTEM_PROMPT = """
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

-----------------------------------
출력 규칙
-----------------------------------

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
      "id": "INC-001",

      "severity": "치명적 | 경고",

      "category": "정책 충돌 | 스키마 불일치 | 워크플로우 충돌 | 네이밍 불일치 | 보안 충돌 | 데이터 타입 충돌 | 요구사항 드리프트 | 제거된 기능 참조 | 기타",

      "title": "<짧은 충돌 제목>",

      "document_1": {
        "reference": "<가능하면 섹션명 또는 제목>",
        "text": "<관련 원문 발췌>"
      },

      "document_2": {
        "reference": "<가능하면 섹션명 또는 제목>",
        "text": "<관련 원문 발췌>"
      },

      "reason": "<왜 충돌하는지에 대한 상세 설명>",

      "impact": "<실제 구현/운영/보안/비즈니스 측면에서 발생 가능한 영향>",

      "fix_patch": {
        "target_document": "문서 1 | 문서 2 | 문서 1 및 문서 2",
        "target_reference": "<수정이 필요한 섹션명 또는 제목>",
        "edit_type": "replace | insert_after | insert_before | delete",
        "original_text": "<수정 대상 원문. delete/replace일 때 필수>",
        "replacement_text": "<문서에 그대로 붙여넣을 수 있는 최종 수정 문장 또는 문단>"
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

10. fix_patch는 단순한 조언이나 방향성이 아니라, 실제 문서에 바로 반영할 수 있는 수정 패치여야 한다.

11. replacement_text에는 “~하는 것이 좋다”, “~해야 한다”, “~로 수정 권장” 같은 제안형 문장을 쓰지 말고, 해당 문서에 그대로 삽입 가능한 완성된 문장 또는 문단만 작성하라.

12. edit_type이 replace인 경우:
- original_text에는 교체 대상 원문을 그대로 넣어라.
- replacement_text에는 교체 후 최종 문장을 넣어라.

13. edit_type이 insert_after 또는 insert_before인 경우:
- original_text에는 삽입 기준이 되는 문장을 넣어라.
- replacement_text에는 새로 삽입할 문장을 넣어라.

14. edit_type이 delete인 경우:
- original_text에는 삭제 대상 원문을 넣어라.
- replacement_text는 빈 문자열 ""로 둔다.

15. 충돌 해결을 위해 어느 문서를 수정해야 할지 판단해야 한다.
- 정책 변경/삭제가 명시된 최신 문서가 있으면, 그 문서를 기준으로 다른 문서를 수정한다.
- 최신 결정사항이 명확하지 않으면, 구현 혼란을 가장 적게 만드는 방향으로 수정 대상을 선택한다.
- 단순히 “문서 간 확인 필요”라고 쓰지 말고, 가능한 한 하나의 구체적인 수정안을 제시한다.


"""

    fun build(
        document1: List<Block>,
        document2: List<Block>,
    ): List<OpenAiChatMessage> = listOf(
        OpenAiChatMessage(OpenAiChatMessage.ROLE_SYSTEM, SYSTEM_PROMPT.trim()),
        OpenAiChatMessage(OpenAiChatMessage.ROLE_USER, userContent(document1, document2)),
    )

    private fun userContent(
        document1: List<Block>,
        document2: List<Block>,
    ): String = buildString {
        append("\n-----------------------------------\n")
        append("문서1\n")
        append("-----------------------------------\n")
        append(serialize(document1))
        append("\n-----------------------------------\n")
        append("문서2\n")
        append("-----------------------------------\n")
        append(serialize(document2))
    }

    private fun serialize(blocks: List<Block>): String =
        if (blocks.isEmpty()) "(없음)"
        else blocks.joinToString("\n") { "[block_id: ${it.blockId}] ${flatten(it.text ?: "")}" }

    private fun flatten(text: String): String =
        text.replace(Regex("\\s+"), " ").trim()
}
