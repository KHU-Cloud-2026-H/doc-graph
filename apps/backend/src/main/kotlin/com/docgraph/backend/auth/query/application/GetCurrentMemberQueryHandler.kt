package com.docgraph.backend.auth.query.application

import org.springframework.stereotype.Service

@Service
class GetCurrentMemberQueryHandler : GetCurrentMemberQuery {
    override fun get(): Long {
        throw UnsupportedOperationException("auth 인증 통합 미구현 — SecurityContext의 OAuth2User → workspace member 매핑 필요")
    }
}