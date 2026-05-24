package com.docgraph.backend.auth.command.domain

fun interface AccessTokenEncryptor {
    fun encrypt(rawToken: String): String
}
