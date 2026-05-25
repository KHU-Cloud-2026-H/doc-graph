package com.docgraph.backend.auth.query.application

fun interface FindUserIdByEmailQuery {
    fun find(email: String): Long?
}
