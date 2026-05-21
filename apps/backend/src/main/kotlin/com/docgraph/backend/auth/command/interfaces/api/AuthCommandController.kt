package com.docgraph.backend.auth.command.interfaces.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Auth")
class AuthCommandController {

    @GetMapping("/oauth2/authorization/notion")
    @Operation(
        summary = "Notion OAuth2 인증 시작",
        description = "Spring Security가 직접 처리하여 Notion authorization URL로 302 redirect한다. 본 핸들러는 spec 문서화용이며 실제 호출되지 않는다.",
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "302", description = "Notion authorization URL로 redirect"),
    ])
    fun oauthStart(): ResponseEntity<Unit> {
        TODO()
    }

    @DeleteMapping("/auth/sessions")
    @Operation(summary = "로그아웃")
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "로그아웃 완료 — 세션 무효화"),
        ApiResponse(responseCode = "401", description = "미인증 상태 (Spring Security)"),
    ])
    fun logout(): ResponseEntity<Unit> {
        TODO()
    }
}
