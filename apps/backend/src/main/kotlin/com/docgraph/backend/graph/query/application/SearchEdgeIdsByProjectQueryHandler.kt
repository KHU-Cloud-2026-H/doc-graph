package com.docgraph.backend.graph.query.application

import org.springframework.stereotype.Service

@Service
class SearchEdgeIdsByProjectQueryHandler : SearchEdgeIdsByProjectQuery {
    override fun search(projectId: Long): List<Long> = TODO("graph 도메인 query 미구현")
}