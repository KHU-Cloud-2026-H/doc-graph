package com.docgraph.backend.auth.query.interfaces.api

import com.docgraph.backend.auth.query.application.UserResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
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
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "사용자 정보"),
        ApiResponse(responseCode = "401", description = "미인증 상태 (Spring Security)"),
    ])
    fun me(): ResponseEntity<UserResponse> {
        TODO()
    }
}
