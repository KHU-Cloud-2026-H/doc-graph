package com.docgraph.backend.project.query.application

/**
 * cross-domain용. validation 인박스 라우팅에서 사용 (target 문서 담당자 부재 시 Project Admin 귀속).
 */
fun interface SearchAdminProjectIdsByUserIdQuery {
    fun search(userId: Long): List<Long>
}
