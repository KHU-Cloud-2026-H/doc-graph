package com.docgraph.backend.auth.query.interfaces.api

import com.docgraph.backend.auth.query.application.UserResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth")
class AuthQueryController {

    @GetMapping("/me")
    @Operation(summary = "현재 로그인 사용자 정보")
    fun me(): ResponseEntity<UserResponse> {
        TODO()
    }
}
