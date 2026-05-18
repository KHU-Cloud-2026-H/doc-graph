package com.docgraph.backend.project.query.application

/**
 * cross-domain용. document worker가 조상 라인 카테고리 매핑 조회 시 사용.
 */
fun interface SearchCategoriesByProjectQuery {
    fun search(projectId: Long): List<CategoryProjection>
}
