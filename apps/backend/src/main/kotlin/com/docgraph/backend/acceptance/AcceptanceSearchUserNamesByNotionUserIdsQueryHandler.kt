package com.docgraph.backend.acceptance

import com.docgraph.backend.auth.query.application.SearchUserNamesByNotionUserIdsQuery
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * acceptance profile 한정 [SearchUserNamesByNotionUserIdsQuery] override.
 *
 * 소비자 [com.docgraph.backend.document.query.application.FindDocumentByIdQueryHandler]가 전 profile에서
 * 본 빈을 요구하므로 context boot용으로 존재한다. acceptance document fixture는 notion_last_edited_by를
 * seed하지 않아 실제로 호출되지 않으며, 호출되더라도 매핑이 없으므로 빈 map을 반환한다.
 */
@Component
@Profile("acceptance")
class AcceptanceSearchUserNamesByNotionUserIdsQueryHandler : SearchUserNamesByNotionUserIdsQuery {
    override fun search(notionUserIds: List<String>): Map<String, String> = emptyMap()
}
