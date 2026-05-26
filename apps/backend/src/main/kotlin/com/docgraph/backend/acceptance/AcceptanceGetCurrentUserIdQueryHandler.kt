package com.docgraph.backend.acceptance

import com.docgraph.backend.auth.command.application.AuthSessionProperties
import com.docgraph.backend.auth.command.domain.SessionTokenService
import com.docgraph.backend.auth.query.application.GetCurrentUserIdQuery
import com.docgraph.backend.auth.query.infra.AuthQueryRepository
import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * acceptance profile 한정 [GetCurrentUserIdQuery] — 운영 세션 쿠키 인증을 acceptance에서 재현.
 *
 * `GET /test/login`이 심은 세션 쿠키([AuthSessionProperties.cookieName])를 운영과 동일하게 해시 →
 * [AuthQueryRepository.findActiveSessionUserIdByTokenHash]로 user id 매핑. 쿠키 부재·만료·revoke 세션은
 * 미인증으로 throw (`/auth/me`가 401 변환).
 *
 * 운영 `GetCurrentUserIdQueryHandler`·`CookieCurrentSessionTokenProvider`는 `@Profile("!acceptance")`
 * (팀원 auth 도메인). 본 빈은 동일 published 컴포넌트(repository·tokenService)를 재사용하되 쿠키 읽기만
 * 인라인 — provider가 acceptance에서 비활성이므로.
 */
@Component
@Profile("acceptance")
class AcceptanceGetCurrentUserIdQueryHandler(
    private val request: HttpServletRequest,
    private val sessionProperties: AuthSessionProperties,
    private val sessionTokenService: SessionTokenService,
    private val authQueryRepository: AuthQueryRepository,
) : GetCurrentUserIdQuery {

    override fun get(): Long {
        val rawToken = request.cookies?.firstOrNull { it.name == sessionProperties.cookieName }?.value
            ?: throw IllegalStateException("acceptance 환경: 세션 쿠키(${sessionProperties.cookieName}) 누락")
        return authQueryRepository.findActiveSessionUserIdByTokenHash(sessionTokenService.hash(rawToken))
            ?: throw IllegalStateException("acceptance 환경: 활성 세션 없음 (쿠키는 있으나 매칭 세션 부재·만료·revoke)")
    }
}
