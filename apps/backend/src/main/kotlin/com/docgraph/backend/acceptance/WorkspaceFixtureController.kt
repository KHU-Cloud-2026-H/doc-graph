package com.docgraph.backend.acceptance

import com.docgraph.backend.auth.query.application.GetCurrentUserIdQuery
import com.docgraph.backend.web.IdResponse
import com.docgraph.backend.workspace.command.application.RegisterWorkspaceCommand
import com.docgraph.backend.workspace.command.application.RegisterWorkspaceCommandHandler
import com.docgraph.backend.workspace.command.domain.WorkspaceMember
import com.docgraph.backend.workspace.command.domain.WorkspaceMemberRepository
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * acceptance profile 한정 — UC spec이 검증 대상 외 workspace 상태를 seed하는 우회 진입점.
 * 운영 흐름은 OAuth 콜백 → `NotionWorkspaceAuthorizedEvent` → workspace listener.
 *
 * `POST /test/workspaces`는 RegisterWorkspaceCommandHandler 호출 — 생성자 workspace_member도 동시 박힘.
 * `POST /test/workspace-members`는 추가 멤버 row 직접 영속.
 */
@RestController
@Profile("acceptance")
class WorkspaceFixtureController(
    private val registerHandler: RegisterWorkspaceCommandHandler,
    private val workspaceMemberRepository: WorkspaceMemberRepository,
    private val getCurrentUserId: GetCurrentUserIdQuery,
) {

    @PostMapping("/test/workspaces")
    fun seedWorkspace(@RequestBody request: SeedWorkspaceRequest): ResponseEntity<IdResponse> {
        val id = registerHandler.handle(
            RegisterWorkspaceCommand(
                ownerUserId = getCurrentUserId.get(),
                notionWorkspaceId = request.notionWorkspaceId,
                notionWorkspaceName = request.name,
                notionBotId = "bot-fixture-${System.nanoTime()}",
            ),
        )
        return ResponseEntity.ok(IdResponse(id))
    }

    @PostMapping("/test/workspace-members")
    @Transactional
    fun seedWorkspaceMember(@RequestBody request: SeedWorkspaceMemberRequest): ResponseEntity<MemberIdResponse> {
        val userId = request.userId ?: (getCurrentUserId.get() + System.nanoTime() % 100000L)
        val saved = workspaceMemberRepository.save(
            WorkspaceMember(workspaceId = request.workspaceId, userId = userId),
        )
        return ResponseEntity.ok(MemberIdResponse(memberId = saved.id))
    }
}

data class SeedWorkspaceRequest(
    @Schema(example = "nw-1")
    val notionWorkspaceId: String,
    @Schema(example = "My Team")
    val name: String,
)

data class SeedWorkspaceMemberRequest(
    @Schema(example = "1")
    val workspaceId: Long,
    @Schema(description = "별 user를 추가할 때 명시. 없으면 fixture가 자동 user_id 생성", example = "1001")
    val userId: Long? = null,
)

data class MemberIdResponse(
    @Schema(example = "1")
    val memberId: Long,
)
