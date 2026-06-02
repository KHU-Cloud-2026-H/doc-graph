package com.docgraph.backend.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

/**
 * 외부로 실제 HTTP를 호출하는 어댑터(및 그 전송용 클라이언트 빈)를 게이팅한다.
 *
 * 운영·acceptance 도커 backend는 무설정으로 활성(matchIfMissing). gradle 테스트 JVM은
 * adapter.http.real=false로 끄고(컨텍스트마다 java.net.http.HttpClient 누적 → OOM 회피),
 * 실 어댑터를 wire로 검증하는 contract 테스트만 adapter.http.real=true로 opt-in한다.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnProperty(name = ["adapter.http.real"], havingValue = "true", matchIfMissing = true)
annotation class ConditionalOnRealHttpAdapter
