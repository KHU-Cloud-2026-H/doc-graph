package com.docgraph.backend.auth.query.application

fun interface CurrentSessionTokenProvider {
    fun get(): String?
}
