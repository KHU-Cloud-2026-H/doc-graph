package com.docgraph.backend.validation.query.application

fun interface GetAiUsageSummaryByProjectQuery {
    fun get(projectId: Long): AiUsageSummaryResponse
}
