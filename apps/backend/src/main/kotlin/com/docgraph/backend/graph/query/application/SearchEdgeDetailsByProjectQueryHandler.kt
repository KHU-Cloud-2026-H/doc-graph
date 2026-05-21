package com.docgraph.backend.graph.query.application

import org.springframework.stereotype.Service

@Service
class SearchEdgeDetailsByProjectQueryHandler : SearchEdgeDetailsByProjectQuery {
    override fun search(projectId: Long): List<EdgeDetail> = TODO("graph 도메인 query 미구현")
}