package com.docgraph.backend.auth.query.application

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("!acceptance")
class SearchUserAccountsByIdsQueryHandler : SearchUserAccountsByIdsQuery {
    override fun search(userIds: List<Long>): List<UserResponse> {
        throw UnsupportedOperationException("auth 영속 미구현 — user batch 조회 필요")
    }
}