package com.docgraph.backend.auth.command.application

data class RevokeSessionCommand(
    val sessionToken: String,
)
