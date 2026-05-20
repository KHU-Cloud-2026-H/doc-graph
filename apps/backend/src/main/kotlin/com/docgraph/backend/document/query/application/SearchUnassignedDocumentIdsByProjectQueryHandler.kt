package com.docgraph.backend.document.query.application

import org.springframework.stereotype.Service

@Service
class SearchUnassignedDocumentIdsByProjectQueryHandler : SearchUnassignedDocumentIdsByProjectQuery {
    override fun search(projectId: Long): List<Long> {
        throw UnsupportedOperationException("document 도메인 구현 미적용 — 담당자 부재 문서 ID 조회")
    }
}
