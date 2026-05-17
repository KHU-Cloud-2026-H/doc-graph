package com.docgraph.backend.auth.query.application

fun interface GetCurrentUserIdQuery {
    fun get(): Long
}