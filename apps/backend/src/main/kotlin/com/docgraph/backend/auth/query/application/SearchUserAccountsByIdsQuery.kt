package com.docgraph.backend.auth.query.application

fun interface SearchUserAccountsByIdsQuery {
    fun search(userIds: List<Long>): List<UserResponse>
}