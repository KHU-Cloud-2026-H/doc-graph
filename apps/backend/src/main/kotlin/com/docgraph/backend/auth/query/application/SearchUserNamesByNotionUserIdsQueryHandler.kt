package com.docgraph.backend.auth.query.application

import com.docgraph.backend.auth.query.infra.AuthQueryRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("!acceptance")
class SearchUserNamesByNotionUserIdsQueryHandler(
    private val repository: AuthQueryRepository,
) : SearchUserNamesByNotionUserIdsQuery {
    override fun search(notionUserIds: List<String>): Map<String, String> =
        repository.searchUserNamesByNotionUserIds(notionUserIds)
}
