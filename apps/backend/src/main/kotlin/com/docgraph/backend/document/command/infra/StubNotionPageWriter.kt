package com.docgraph.backend.document.command.infra

import com.docgraph.backend.document.command.domain.NotionPageWriter
import com.docgraph.backend.document.command.domain.NotionPatchResult
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 운영 NotionPageWriter 어댑터(팀원 axis) 부재 상태에서 ProposalApprovedEventListener
 * wiring을 boot 통과시키기 위한 placeholder. 운영 impl 등록 시 자동 비활성.
 *
 * UC4 acceptance 통과는 운영 어댑터가 wiremock으로 PATCH 호출해야 가능. 본 stub은 호출만
 * Success 반환 — wiremock에 PATCH가 닿지 않아 UC4는 여전히 timeout.
 */
@Configuration
class StubNotionPageWriterConfig {
    @Bean
    @ConditionalOnMissingBean(NotionPageWriter::class)
    fun stubNotionPageWriter(): NotionPageWriter =
        NotionPageWriter { _, _, _ -> NotionPatchResult.Success }
}
