package com.docgraph.backend.project.query.application

fun interface SearchTypeAssigneeDefaultsByProjectQuery {
    /** null = project 없음 또는 접근 권한 없음 */
    fun search(projectId: Long, userId: Long): List<TypeAssigneeResponse>?
}
