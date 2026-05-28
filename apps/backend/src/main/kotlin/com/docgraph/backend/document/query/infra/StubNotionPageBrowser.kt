package com.docgraph.backend.document.query.infra

import com.docgraph.backend.document.query.application.NotionPageBrowser
import com.docgraph.backend.document.query.application.NotionPageRef
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 운영 Notion read 어댑터(Notion API client) 부재 상태에서 boot·프론트 통합을 통과시키기 위한 placeholder.
 * 운영 impl 등록 시 자동 비활성. 실제 Notion 호출 없이 빈 목록 반환.
 */
@Configuration
class StubNotionPageBrowserConfig {
    @Bean
    @ConditionalOnMissingBean(NotionPageBrowser::class)
    fun stubNotionPageBrowser(): NotionPageBrowser = object : NotionPageBrowser {
        override fun listRootPages(workspaceId: Long): List<NotionPageRef> = emptyList()
        override fun listChildPages(workspaceId: Long, parentNotionPageId: String): List<NotionPageRef> = emptyList()
    }
}
