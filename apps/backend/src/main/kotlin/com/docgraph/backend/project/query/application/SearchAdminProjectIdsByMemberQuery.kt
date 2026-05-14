package com.docgraph.backend.project.query.application

fun interface SearchAdminProjectIdsByMemberQuery {
    fun search(memberId: Long): List<Long>
}
