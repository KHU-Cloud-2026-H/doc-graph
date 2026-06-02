package com.docgraph.backend.fixtures

import org.springframework.test.context.TestPropertySource

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@TestPropertySource(
    properties = [
        "ai.openai.api-key=test",
        "ai.openai.model=test-model",
        // 실 OpenAI 어댑터를 wire(WireMock)로 검증하는 contract 테스트 — 실 RestClient opt-in.
        "adapter.http.real=true",
    ]
)
annotation class OpenAiTestFixture
