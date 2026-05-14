package com.docgraph.backend.graph.query.application

import org.springframework.stereotype.Service

@Service
class SearchEdgeIdsByTargetDocumentIdsQueryHandler : SearchEdgeIdsByTargetDocumentIdsQuery {
    override fun search(targetDocumentIds: List<Long>): List<Long> {
        throw UnsupportedOperationException("graph 도메인 구현 미적용 — target 문서 기준 엣지 ID 조회")
    }
}
