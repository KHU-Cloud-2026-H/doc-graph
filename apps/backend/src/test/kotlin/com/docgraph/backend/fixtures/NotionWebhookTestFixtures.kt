package com.docgraph.backend.fixtures

import org.springframework.test.context.TestPropertySource

/** HMAC 검증 활성 테스트가 서명 계산에 쓰는 고정 secret. 애너테이션·테스트 본문 단일 출처. */
const val NOTION_WEBHOOK_TEST_SECRET = "hmac-test-secret"

/**
 * webhook HMAC 검증 비활성(secret blank). 주입 환경(.env.local 등)에 NOTION_WEBHOOK_SECRET이 있어도
 * 서명 없는 요청이 401나지 않도록 고정 — HMAC 외 webhook 처리 경로 검증용.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@TestPropertySource(properties = ["notion.webhook.secret="])
annotation class NotionWebhookHmacDisabled

/** webhook HMAC 검증 활성(고정 test secret). 서명 검증 자체를 검증용. */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@TestPropertySource(properties = ["notion.webhook.secret=$NOTION_WEBHOOK_TEST_SECRET"])
annotation class NotionWebhookHmacEnabled
