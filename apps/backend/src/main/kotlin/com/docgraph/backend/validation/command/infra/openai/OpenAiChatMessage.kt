package com.docgraph.backend.validation.command.infra.openai

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OpenAiChatMessage(
    val role: String,
    // 요청 시엔 항상 채워 보내지만, 응답에선 refusal·length 종료 등으로 null일 수 있어 nullable.
    val content: String? = null,
    // 구조화 출력 거부 시 OpenAI가 content 대신 채우는 사유. 요청 직렬화엔 NON_NULL로 미포함.
    val refusal: String? = null,
) {
    companion object {
        const val ROLE_SYSTEM = "system"
        const val ROLE_USER = "user"
    }
}
