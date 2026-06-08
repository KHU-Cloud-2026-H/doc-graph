package com.docgraph.backend.auth.query.application

import com.docgraph.backend.auth.query.infra.AuthQueryRepository
import org.springframework.stereotype.Service

@Service
class SearchUserAccountsByIdsQueryHandler(
    private val repository: AuthQueryRepository,
) : SearchUserAccountsByIdsQuery {
    override fun search(userIds: List<Long>): List<UserResponse> =
        repository.searchUserAccountsByIds(userIds)
}
