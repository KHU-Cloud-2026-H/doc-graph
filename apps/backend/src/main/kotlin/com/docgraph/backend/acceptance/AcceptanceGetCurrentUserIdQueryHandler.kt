package com.docgraph.backend.acceptance

import com.docgraph.backend.auth.query.application.GetCurrentUserIdQuery
import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * acceptance profile 한정 [GetCurrentUserIdQuery] override — OAuth2/auth 영속 미구현 동안의 우회.
 *
 * `X-Test-User-Id` 헤더의 Long 값을 그대로 반환. 헤더 누락·parse 실패는 fixture 작성 오류로 간주하여 throw.
 *
 * 교체 조건: auth User entity 영속화 + OAuth2 통합 완성 시 본 빈 제거 → `POST /test/users` row endpoint
 * + SecurityContext 주입 filter 조합으로 전환. 그래야 운영 매핑 코드가 acceptance에서도 동일하게
 * 검증된다 (docs/testing.md: "같은 코드가 양쪽에서 동작").
 */
@Component
@Profile("acceptance")
class AcceptanceGetCurrentUserIdQueryHandler(
    private val request: HttpServletRequest,
) : GetCurrentUserIdQuery {

    override fun get(): Long {
        val header = request.getHeader(HEADER_NAME)
            ?: throw IllegalStateException("acceptance 환경: $HEADER_NAME 헤더 누락")
        return header.toLongOrNull()
            ?: throw IllegalStateException("acceptance 환경: $HEADER_NAME 헤더 값이 Long 아님 ($header)")
    }

    companion object {
        const val HEADER_NAME = "X-Test-User-Id"
    }
}