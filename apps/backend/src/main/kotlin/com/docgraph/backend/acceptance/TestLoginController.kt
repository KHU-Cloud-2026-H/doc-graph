package com.docgraph.backend.acceptance

import com.docgraph.backend.auth.command.application.AuthSessionProperties
import com.docgraph.backend.auth.command.domain.SessionTokenService
import com.docgraph.backend.auth.command.domain.UserAccount
import com.docgraph.backend.auth.command.domain.UserAccountRepository
import com.docgraph.backend.auth.command.domain.UserSession
import com.docgraph.backend.auth.command.domain.UserSessionRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.time.OffsetDateTime

/**
 * acceptance profile 한정 — OAuth/Notion 부재 시 세션 쿠키 로그인 우회.
 *
 * 운영 OAuth 콜백(`GET /login/oauth2/code/notion`)과 동형: 실제 [UserSession] 발급 + 세션 쿠키 Set +
 * `auth.oauth.success-redirect-uri` redirect. 브라우저는 `/api/test/login` 진입만으로 로그인되고(프론트
 * 무변경), seed-fixture·pytest는 동일 엔드포인트로 쿠키 확보. acceptance `GetCurrentUserIdQuery`가 이
 * 쿠키를 운영과 동일 query로 user id에 매핑.
 *
 * `key`별 정규 test user를 get-or-create(notion_user_id=`acceptance-{key}`) — 같은 key = 같은 user
 * (브라우저·seed 공유), 다른 key = 다른 user (UC8 담당자 가장). user id는 IDENTITY 생성 — 호출자는
 * `GET /auth/me`로 조회. (V17 `user_session.user_id` FK 충족 위해 row 영속.)
 */
@RestController
@Profile("acceptance")
@Tag(name = "TestLogin")
class TestLoginController(
    private val userAccountRepository: UserAccountRepository,
    private val userSessionRepository: UserSessionRepository,
    private val sessionTokenService: SessionTokenService,
    private val sessionProperties: AuthSessionProperties,
    @Value("\${auth.oauth.success-redirect-uri}")
    private val successRedirectUri: String,
) {

    @GetMapping("/test/login")
    @Operation(summary = "acceptance 세션 쿠키 로그인 우회")
    @ApiResponses(value = [
        ApiResponse(responseCode = "302", description = "세션 쿠키 Set 후 success redirect URI로 redirect"),
    ])
    @Transactional
    fun login(
        @RequestParam(defaultValue = "default") key: String,
        request: HttpServletRequest,
    ): ResponseEntity<Unit> {
        val now = OffsetDateTime.now()
        val notionUserId = "acceptance-$key"
        val user = userAccountRepository.findByNotionUserId(notionUserId)
            ?: userAccountRepository.save(
                UserAccount.registerFromNotion(
                    notionUserId = notionUserId,
                    email = "$key@acceptance.test",
                    name = "Acceptance $key",
                    avatarUrl = null,
                    now = now,
                ),
            )
        val rawToken = sessionTokenService.generate()
        userSessionRepository.save(
            UserSession(
                userId = user.id,
                tokenHash = sessionTokenService.hash(rawToken),
                expiresAt = now.plusSeconds(sessionProperties.ttlSeconds),
                createdAt = now,
            ),
        )
        val cookie = ResponseCookie.from(sessionProperties.cookieName, rawToken)
            .httpOnly(true)
            .secure(request.isSecure)
            .path("/")
            .sameSite("Lax")
            .maxAge(sessionProperties.ttlSeconds)
            .build()
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(successRedirectUri))
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .build()
    }
}
