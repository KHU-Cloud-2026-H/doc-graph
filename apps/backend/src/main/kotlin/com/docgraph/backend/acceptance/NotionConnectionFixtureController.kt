package com.docgraph.backend.acceptance

import com.docgraph.backend.auth.command.domain.AccessTokenEncryptor
import com.docgraph.backend.auth.command.domain.NotionConnection
import com.docgraph.backend.auth.command.domain.NotionConnectionRepository
import com.docgraph.backend.auth.query.application.GetCurrentUserIdQuery
import com.docgraph.backend.web.IdResponse
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * acceptance profile 한정 — UC spec이 검증 대상 외 Notion 연결(access token) 상태를 seed하는 우회 진입점.
 * 운영 흐름은 OAuth 콜백 → `CompleteNotionOAuthCommandHandler`가 토큰 암호화·`NotionConnection` 영속.
 *
 * Notion read/write 어댑터는 `NotionConnection`의 복호화된 access token으로 호출한다. local-mock엔
 * OAuth 흐름이 없어 연결이 안 박히므로, 운영과 동일한 `AccessTokenEncryptor`로 더미 토큰을 암호화해
 * 직접 영속한다(복호화는 운영 `NotionAccessTokenDecryptor` 경로 그대로 통과). 현재 user는 세션에서 해석.
 */
@RestController
@Profile("acceptance")
class NotionConnectionFixtureController(
    private val notionConnectionRepository: NotionConnectionRepository,
    private val accessTokenEncryptor: AccessTokenEncryptor,
    private val getCurrentUserId: GetCurrentUserIdQuery,
) {

    @PostMapping("/test/notion-connections")
    fun seedConnection(@RequestBody request: SeedNotionConnectionRequest): ResponseEntity<IdResponse> {
        val saved = notionConnectionRepository.save(
            NotionConnection(
                userId = getCurrentUserId.get(),
                notionWorkspaceId = request.notionWorkspaceId,
                notionWorkspaceName = request.notionWorkspaceName,
                notionBotId = "bot-fixture-${System.nanoTime()}",
                accessTokenEncrypted = accessTokenEncryptor.encrypt(request.accessToken),
                tokenType = "bearer",
            ),
        )
        return ResponseEntity.ok(IdResponse(saved.id))
    }
}

data class SeedNotionConnectionRequest(
    @Schema(description = "연결할 Notion workspace ID (workspace.notionWorkspaceId와 일치)", example = "nw-1")
    val notionWorkspaceId: String,
    @Schema(description = "암호화해 저장할 raw access token (어댑터가 복호화해 사용)", example = "fixture-access-token")
    val accessToken: String = "fixture-access-token",
    @Schema(example = "My Team")
    val notionWorkspaceName: String = "Fixture Workspace",
)