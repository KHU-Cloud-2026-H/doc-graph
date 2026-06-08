package com.docgraph.backend.document.command.interfaces.event

import com.docgraph.backend.document.command.application.ProcessDocumentChangeNoticeCommandHandler
import com.docgraph.backend.document.command.domain.DocumentChangeNoticeQueuedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Recover
import org.springframework.retry.annotation.Retryable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException

@Component
class DocumentChangeNoticeQueuedEventListener(
    private val handler: ProcessDocumentChangeNoticeCommandHandler,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @EventListener
    @Retryable(
        // transient 실패만 재시도: 429(rate limit)·5xx·연결/읽기 타임아웃.
        // 비-429 4xx(인증·권한·삭제), 응답 파싱 실패 등은 재호출해도 결과가 같으므로
        // 재시도하지 않고 Recover로 직행한다.
        retryFor = [
            HttpServerErrorException::class,
            HttpClientErrorException.TooManyRequests::class,
            ResourceAccessException::class,
        ],
        maxAttemptsExpression = "\${document.change-notice.fetch.max-attempts:5}",
        backoff = Backoff(
            delayExpression = "\${document.change-notice.fetch.retry-delay-ms:1000}",
            multiplierExpression = "\${document.change-notice.fetch.retry-multiplier:2.0}",
            maxDelayExpression = "\${document.change-notice.fetch.retry-max-delay-ms:60000}",
            random = true,
        ),
    )
    fun on(event: DocumentChangeNoticeQueuedEvent) {
        val notice = handler.recordAttempt(event.noticeId) ?: return
        val result = handler.fetchFromNotion(notice)
        handler.applyAndMarkSuccess(notice, result)
    }

    @Recover
    fun recover(ex: Throwable, event: DocumentChangeNoticeQueuedEvent) {
        log.error(
            "문서 변경 통지 처리 실패 — 재시도 소진, 실패 기록: noticeId={}: {}",
            event.noticeId, ex.message, ex,
        )
        handler.markFailed(event.noticeId, ex.message)
    }
}
