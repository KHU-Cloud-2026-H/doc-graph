package com.docgraph.backend.auth.query.interfaces.api

import com.docgraph.backend.auth.query.application.GetCurrentUserIdQuery
import com.docgraph.backend.auth.query.application.SearchUserAccountsByIdsQuery
import com.docgraph.backend.auth.query.application.UserResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth")
class AuthQueryController(
    private val getCurrentUserId: GetCurrentUserIdQuery,
    private val searchUserAccountsByIds: SearchUserAccountsByIdsQuery,
) {

    @GetMapping("/me", produces = [MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"])
    @Operation(summary = "현재 로그인 사용자 정보")
    fun me(): ResponseEntity<UserResponse> {
        val userId = try {
            getCurrentUserId.get()
        } catch (_: IllegalStateException) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        val user = searchUserAccountsByIds.search(listOf(userId)).firstOrNull()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        return ResponseEntity.ok(user)
    }
}
