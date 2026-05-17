package com.docgraph.backend.auth.query.application

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("!acceptance")
class FindUserIdByEmailQueryHandler : FindUserIdByEmailQuery {
    override fun find(email: String): Long? {
        throw UnsupportedOperationException("auth 영속 미구현 — member email index 조회 필요")
    }
}
