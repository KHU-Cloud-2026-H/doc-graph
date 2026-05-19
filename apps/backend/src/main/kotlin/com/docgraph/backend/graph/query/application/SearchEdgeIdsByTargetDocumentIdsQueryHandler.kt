package com.docgraph.backend.graph.query.application

import com.docgraph.backend.graph.command.domain.DependencyEdgeRepository
import org.springframework.stereotype.Service

@Service
class SearchEdgeIdsByTargetDocumentIdsQueryHandler(
    private val edgeRepository: DependencyEdgeRepository,
) : SearchEdgeIdsByTargetDocumentIdsQuery {
    override fun search(targetDocumentIds: List<Long>): List<Long> =
        edgeRepository.findAllByTargetDocumentIdIn(targetDocumentIds).map { it.id }
}
