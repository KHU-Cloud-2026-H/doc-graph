package com.docgraph.backend.event

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
@EnableAsync
class AsyncConfig {
    // @Async는 @Scheduled가 만드는 taskScheduler와 함께 TaskExecutor 후보가 둘이 되어 실행기를 결정하지 못한다.
    // @Primary로 이 풀을 @Async 기본 실행기로 못박는다 (미지정 시 Spring이 fallback 실행기로 떨어져 디스패치가 불안정).
    @Bean
    @Primary
    fun applicationTaskExecutor(): ThreadPoolTaskExecutor = ThreadPoolTaskExecutor().apply {
        corePoolSize = 4
        maxPoolSize = 8
        queueCapacity = 100
        setThreadNamePrefix("event-")
        initialize()
    }
}
