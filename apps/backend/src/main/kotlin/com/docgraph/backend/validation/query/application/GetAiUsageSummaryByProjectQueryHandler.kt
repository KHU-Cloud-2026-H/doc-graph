package com.docgraph.backend.validation.query.application

import com.docgraph.backend.validation.query.infra.AiUsageQueryRepository
import org.springframework.stereotype.Service

@Service
class GetAiUsageSummaryByProjectQueryHandler(
    private val repository: AiUsageQueryRepository,
) : GetAiUsageSummaryByProjectQuery {

    override fun get(projectId: Long): AiUsageSummaryResponse {
        val byModel = repository.aggregateByModel(projectId)
        // 합계는 모델별 분해의 합 — 별도 집계 쿼리 없이 도출.
        return AiUsageSummaryResponse(
            totalCalls = byModel.sumOf { it.calls },
            totalPromptTokens = byModel.sumOf { it.promptTokens },
            totalCompletionTokens = byModel.sumOf { it.completionTokens },
            totalTokens = byModel.sumOf { it.totalTokens },
            byModel = byModel,
        )
    }
}
