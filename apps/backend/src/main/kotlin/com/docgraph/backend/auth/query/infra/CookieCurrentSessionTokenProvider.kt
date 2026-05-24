package com.docgraph.backend.auth.query.infra

import com.docgraph.backend.auth.command.application.AuthSessionProperties
import com.docgraph.backend.auth.query.application.CurrentSessionTokenProvider
import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!acceptance")
class CookieCurrentSessionTokenProvider(
    private val request: HttpServletRequest,
    private val properties: AuthSessionProperties,
) : CurrentSessionTokenProvider {
    override fun get(): String? =
        request.cookies?.firstOrNull { it.name == properties.cookieName }?.value
}
