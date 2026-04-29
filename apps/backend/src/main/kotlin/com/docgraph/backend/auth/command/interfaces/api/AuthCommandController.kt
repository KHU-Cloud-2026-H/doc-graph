package com.docgraph.backend.auth.command.interfaces.api

import io.swagger.v3.oas.annotations.Operation
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
    @Operation(summary = "Notion OAuth2 인증 시작 (Spring Security가 처리 — 브라우저 redirect)")
    fun oauthStart(): ResponseEntity<Unit> {
        TODO()
    }

    @DeleteMapping("/auth/sessions")
    @Operation(summary = "로그아웃")
    fun logout(): ResponseEntity<Unit> {
        TODO()
    }
}
