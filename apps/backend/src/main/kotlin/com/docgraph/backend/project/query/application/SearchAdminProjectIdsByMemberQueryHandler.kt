package com.docgraph.backend.project.query.application

import org.springframework.stereotype.Service

@Service
class SearchAdminProjectIdsByMemberQueryHandler : SearchAdminProjectIdsByMemberQuery {
    override fun search(memberId: Long): List<Long> {
        throw UnsupportedOperationException("project 도메인 구현 미적용 — Admin 프로젝트 ID 조회")
    }
}
