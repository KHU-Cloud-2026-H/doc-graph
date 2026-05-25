package com.docgraph.backend.auth.command.application

import com.docgraph.backend.auth.command.domain.SessionTokenService
import com.docgraph.backend.auth.command.domain.UserSessionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RevokeSessionCommandHandler(
    private val userSessionRepository: UserSessionRepository,
    private val sessionTokenService: SessionTokenService,
) {
    @Transactional
    fun handle(command: RevokeSessionCommand) {
        val session = userSessionRepository.findByTokenHash(sessionTokenService.hash(command.sessionToken)) ?: return
        session.revoke()
        userSessionRepository.save(session)
    }
}
