package com.docgraph.backend.validation.command.interfaces.event

import com.docgraph.backend.validation.command.domain.ConflictDetectionResponseException
import com.docgraph.backend.validation.command.domain.FailureCategory
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException

/**
 * AI 검증 실패의 throwable을 [FailureCategory]로 분류한다. 전송 계층(Spring Web)·응답
 * 계층(도메인 예외)을 운영 대응 단위로 환원한다. 분류되지 않는 내부 오류는 UNKNOWN.
 */
object ValidationFailureClassifier {

    fun classify(ex: Throwable): FailureCategory = when (ex) {
        is ConflictDetectionResponseException -> FailureCategory.INVALID_RESPONSE
        is HttpClientErrorException.TooManyRequests -> FailureCategory.RATE_LIMITED
        is HttpClientErrorException -> FailureCategory.INVALID_REQUEST
        is HttpServerErrorException -> FailureCategory.UPSTREAM_ERROR
        is ResourceAccessException -> FailureCategory.TIMEOUT
        else -> FailureCategory.UNKNOWN
    }
}