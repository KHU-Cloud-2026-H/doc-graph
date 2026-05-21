package com.docgraph.backend.auth.command.application

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("auth.session")
data class AuthSessionProperties(
    val cookieName: String = "DG_SESSION",
    val ttlSeconds: Long = 60 * 60 * 24 * 7,
)
