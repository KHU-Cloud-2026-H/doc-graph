package com.docgraph.backend.validation.query.application

import com.docgraph.backend.graph.query.application.FindEdgeByIdQuery
import com.docgraph.backend.validation.query.infra.ValidationQueryRepository
import org.springframework.stereotype.Service

@Service
class FindConflictFindingByIdQueryHandler(
    private val validationQueryRepository: ValidationQueryRepository,
    private val findEdgeById: FindEdgeByIdQuery,
) : FindConflictFindingByIdQuery {

    override fun find(findingId: Long): ConflictFindingDetail? {
        val row = validationQueryRepository.findFindingDetailById(findingId) ?: return null
        val edge = findEdgeById.find(row.edgeId)
            ?: error("edge not found for finding $findingId: edgeId=${row.edgeId}")
        return ConflictFindingDetail(
            findingId = row.findingId,
            targetDocumentId = edge.targetDocumentId,
            targetBlockId = row.targetBlockId,
            newText = row.newText,
        )
    }
}
