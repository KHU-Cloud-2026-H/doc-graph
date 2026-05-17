package com.docgraph.backend.acceptance

import com.docgraph.backend.web.IdResponse
import com.docgraph.backend.workspace.command.application.RegisterWorkspaceCommand
import com.docgraph.backend.workspace.command.application.RegisterWorkspaceCommandHandler
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * acceptance profile 한정 — UC4~UC8 spec이 검증 대상 외 workspace 상태를 seed하는 우회 진입점.
 * 운영 흐름은 OAuth 콜백 → `NotionWorkspaceAuthorizedEvent` → workspace listener.
 */
@RestController
@Profile("acceptance")
class WorkspaceFixtureController(
    private val registerHandler: RegisterWorkspaceCommandHandler,
) {

    @PostMapping("/test/workspaces")
    fun seed(@RequestBody request: SeedWorkspaceRequest): ResponseEntity<IdResponse> {
        val id = registerHandler.handle(
            RegisterWorkspaceCommand(
                ownerUserId = request.ownerUserId,
                notionWorkspaceId = request.notionWorkspaceId,
                notionWorkspaceName = request.notionWorkspaceName,
                notionBotId = request.notionBotId,
            ),
        )
        return ResponseEntity.ok(IdResponse(id))
    }
}

data class SeedWorkspaceRequest(
    @Schema(example = "1")
    val ownerUserId: Long,
    @Schema(example = "nw-1")
    val notionWorkspaceId: String,
    @Schema(example = "My Team")
    val notionWorkspaceName: String,
    @Schema(example = "bot-1")
    val notionBotId: String,
)