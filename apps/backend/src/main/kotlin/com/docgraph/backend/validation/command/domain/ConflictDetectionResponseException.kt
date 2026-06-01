package com.docgraph.backend.validation.command.domain

/**
 * AI가 HTTP 200으로 응답했으나 결과를 쓸 수 없는 경우 — 스키마 불일치·파싱 불가·빈 응답.
 * 전송 오류(4xx/5xx/timeout)와 구분되는 응답 오류이며, 재호출해도 같은 입력이면
 * 결과가 같다고 보아 재시도하지 않는다(terminal). [FailureCategory.INVALID_RESPONSE]로 분류된다.
 */
class ConflictDetectionResponseException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)