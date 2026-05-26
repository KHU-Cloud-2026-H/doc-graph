package com.docgraph.backend.notification.command.domain

/**
 * 프로젝트 Webhook URL로 알림을 발송하는 외부 시스템 port. 구현체는 infra.
 * 발송 실패는 best-effort — 충돌 인앱 표시는 validation이 유지하므로 호출자에게 예외를 전파하지 않는다.
 */
fun interface WebhookNotifier {
    fun send(url: String, message: String)
}
