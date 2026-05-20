package com.docgraph.backend.auth.query.application

fun interface GetCurrentMemberQuery {
    fun get(): Long
}