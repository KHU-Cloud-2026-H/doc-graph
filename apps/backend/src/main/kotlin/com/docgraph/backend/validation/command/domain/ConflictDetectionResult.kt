package com.docgraph.backend.validation.command.domain

/**
 * 충돌 검출 1회의 산출물. 검출 결과와 호출 사용량을 함께 반환한다.
 * usage는 응답이 사용량을 알려준 경우에만 채워진다(관측은 검출을 막지 않는 best-effort).
 */
data class ConflictDetectionResult(
    val conflicts: List<DetectedConflict>,
    val usage: AiUsage?,
)
