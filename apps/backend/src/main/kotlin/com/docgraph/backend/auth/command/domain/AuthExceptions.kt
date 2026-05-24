package com.docgraph.backend.auth.command.domain

class AuthSessionInactiveException(sessionId: Long) : RuntimeException(
    "비활성 세션입니다. sessionId=$sessionId",
)
