package com.docgraph.backend.auth.command.interfaces.api

import com.docgraph.backend.auth.command.application.AuthSessionProperties
import com.docgraph.backend.auth.command.application.CompleteNotionOAuthCommand
import com.docgraph.backend.auth.command.application.CompleteNotionOAuthCommandHandler
import com.docgraph.backend.auth.command.application.RevokeSessionCommand
import com.docgraph.backend.auth.command.application.RevokeSessionCommandHandler
import com.docgraph.backend.auth.command.application.StartNotionOAuthCommand
import com.docgraph.backend.auth.command.application.StartNotionOAuthCommandHandler
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@Tag(name = "Auth")
class AuthCommandController(
    private val startNotionOAuth: StartNotionOAuthCommandHandler,
    private val completeNotionOAuth: CompleteNotionOAuthCommandHandler,
    private val revokeSession: RevokeSessionCommandHandler,
    private val sessionProperties: AuthSessionProperties,
    @Value("\${auth.oauth.success-redirect-uri:/workspaces}")
    private val successRedirectUri: String,
) {

    @GetMapping("/oauth2/authorization/notion")
    @Operation(summary = "Notion OAuth2 인증 시작")
    @ApiResponses(value = [
        ApiResponse(responseCode = "302", description = "Notion authorization URL로 redirect"),
    ])
    fun oauthStart(@RequestParam(required = false) state: String?): ResponseEntity<Unit> {
        val authorizationUrl = startNotionOAuth.handle(StartNotionOAuthCommand(state))
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(authorizationUrl))
            .build()
    }

    @GetMapping("/login/oauth2/code/notion")
    @Operation(summary = "Notion OAuth2 콜백")
    @ApiResponses(value = [
        ApiResponse(responseCode = "302", description = "성공 시 success redirect URI로, 실패 시 동 URI에 `?oauth=error` 부착"),
    ])
    fun oauthCallback(
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) error: String?,
        request: HttpServletRequest,
    ): ResponseEntity<Unit> {
        if (code.isNullOrBlank() || !error.isNullOrBlank()) {
            return redirect("$successRedirectUri?oauth=error").build()
        }

        val result = completeNotionOAuth.handle(CompleteNotionOAuthCommand(code))
        val cookie = sessionCookie(
            request = request,
            value = result.sessionToken,
            maxAgeSeconds = result.sessionExpiresInSeconds,
        )
        return redirect(successRedirectUri)
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .build()
    }

    @DeleteMapping("/auth/sessions")
    @Operation(summary = "로그아웃")
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "로그아웃 완료 — 세션 무효화 및 쿠키 만료"),
    ])
    fun logout(request: HttpServletRequest): ResponseEntity<Unit> {
        request.cookies?.firstOrNull { it.name == sessionProperties.cookieName }?.value?.let { token ->
            revokeSession.handle(RevokeSessionCommand(token))
        }
        val expiredCookie = sessionCookie(request = request, value = "", maxAgeSeconds = 0)
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
            .build()
    }

    private fun redirect(uri: String): ResponseEntity.BodyBuilder =
        ResponseEntity.status(HttpStatus.FOUND).location(URI.create(uri))

    private fun sessionCookie(
        request: HttpServletRequest,
        value: String,
        maxAgeSeconds: Long,
    ): ResponseCookie =
        ResponseCookie.from(sessionProperties.cookieName, value)
            .httpOnly(true)
            .secure(request.isSecure)
            .path("/")
            .sameSite("Lax")
            .maxAge(maxAgeSeconds)
            .build()
}
