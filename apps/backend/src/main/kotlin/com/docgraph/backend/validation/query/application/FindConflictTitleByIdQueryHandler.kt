package com.docgraph.backend.validation.query.application

import com.docgraph.backend.validation.query.infra.ValidationQueryRepository
import org.springframework.stereotype.Service

@Service
class FindConflictTitleByIdQueryHandler(
    private val validationQueryRepository: ValidationQueryRepository,
) : FindConflictTitleByIdQuery {
    override fun find(conflictId: Long): String? =
        validationQueryRepository.findLatestFindingTitleByConflictId(conflictId)
}
