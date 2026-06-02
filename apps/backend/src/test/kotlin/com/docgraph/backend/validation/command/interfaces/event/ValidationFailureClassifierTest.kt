package com.docgraph.backend.validation.command.interfaces.event

import com.docgraph.backend.validation.command.domain.ConflictDetectionResponseException
import com.docgraph.backend.validation.command.domain.FailureCategory
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import java.io.IOException
import kotlin.test.assertEquals

@Tag("unit")
class ValidationFailureClassifierTest {

    @Test
    fun `429 - RATE_LIMITED`() {
        val ex = HttpClientErrorException.create(
            HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", HttpHeaders.EMPTY, ByteArray(0), null,
        )
        assertEquals(FailureCategory.RATE_LIMITED, ValidationFailureClassifier.classify(ex))
    }

    @Test
    fun `비-429 4xx - INVALID_REQUEST`() {
        assertEquals(
            FailureCategory.INVALID_REQUEST,
            ValidationFailureClassifier.classify(HttpClientErrorException(HttpStatus.BAD_REQUEST)),
        )
        assertEquals(
            FailureCategory.INVALID_REQUEST,
            ValidationFailureClassifier.classify(HttpClientErrorException(HttpStatus.UNAUTHORIZED)),
        )
    }

    @Test
    fun `5xx - UPSTREAM_ERROR`() {
        assertEquals(
            FailureCategory.UPSTREAM_ERROR,
            ValidationFailureClassifier.classify(HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)),
        )
    }

    @Test
    fun `타임아웃 - TIMEOUT`() {
        assertEquals(
            FailureCategory.TIMEOUT,
            ValidationFailureClassifier.classify(ResourceAccessException("read timed out", IOException("timeout"))),
        )
    }

    @Test
    fun `응답 오류 - INVALID_RESPONSE`() {
        assertEquals(
            FailureCategory.INVALID_RESPONSE,
            ValidationFailureClassifier.classify(ConflictDetectionResponseException("스키마 불일치")),
        )
    }

    @Test
    fun `분류 불가 - UNKNOWN`() {
        assertEquals(FailureCategory.UNKNOWN, ValidationFailureClassifier.classify(RuntimeException("boom")))
    }
}
