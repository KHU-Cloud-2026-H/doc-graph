package com.docgraph.backend.auth.query.application

import com.docgraph.backend.auth.command.domain.SessionTokenService
import com.docgraph.backend.auth.query.infra.AuthQueryRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("!acceptance")
class GetCurrentUserIdQueryHandler(
    private val currentSessionTokenProvider: CurrentSessionTokenProvider,
    private val sessionTokenService: SessionTokenService,
    private val repository: AuthQueryRepository,
) : GetCurrentUserIdQuery {
    override fun get(): Long {
        val rawToken = currentSessionTokenProvider.get()
            ?: throw IllegalStateException("authenticated session cookie is missing")
        return repository.findActiveSessionUserIdByTokenHash(sessionTokenService.hash(rawToken))
            ?: throw IllegalStateException("authenticated session is not found")
    }
}
