package com.docgraph.backend.validation.command.domain

/**
 * ValidationTask 실패 원인 분류. AI 호출의 실패 양상을 운영 대응 단위로 묶는다.
 * - RATE_LIMITED: 429. 호출 빈도 초과.
 * - TIMEOUT: 연결/읽기 타임아웃.
 * - UPSTREAM_ERROR: AI 서버 5xx.
 * - INVALID_REQUEST: 비-429 4xx. 잘못된 요청·인증·설정.
 * - INVALID_RESPONSE: AI가 응답했으나 스키마 불일치·파싱 불가·빈 응답.
 * - MISSING_REFERENCE: 검증 준비 단계에서 참조 엣지·문서를 찾을 수 없음(AI 호출 이전).
 * - UNKNOWN: 분류되지 않은 내부 오류.
 */
enum class FailureCategory {
    RATE_LIMITED,
    TIMEOUT,
    UPSTREAM_ERROR,
    INVALID_REQUEST,
    INVALID_RESPONSE,
    MISSING_REFERENCE,
    UNKNOWN,
}