package com.docgraph.backend.document.query.application

import org.springframework.stereotype.Service

@Service
class SearchDocumentIdsByAssigneeQueryHandler : SearchDocumentIdsByAssigneeQuery {
    override fun search(memberId: Long): List<Long> {
        throw UnsupportedOperationException("document 도메인 구현 미적용 — 담당 문서 ID 조회")
    }
}
