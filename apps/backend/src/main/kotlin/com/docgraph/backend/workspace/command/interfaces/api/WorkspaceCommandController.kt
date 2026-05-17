package com.docgraph.backend.workspace.command.interfaces.api

import com.docgraph.backend.auth.query.application.GetCurrentUserIdQuery
import com.docgraph.backend.web.IdResponse
import com.docgraph.backend.workspace.command.application.InviteWorkspaceMemberCommand
import com.docgraph.backend.workspace.command.application.InviteWorkspaceMemberCommandHandler
import com.docgraph.backend.workspace.command.application.RemoveWorkspaceMemberCommand
import com.docgraph.backend.workspace.command.application.RemoveWorkspaceMemberCommandHandler
import com.docgraph.backend.workspace.command.domain.WorkspaceCreatorRemovalException
import com.docgraph.backend.workspace.command.domain.WorkspaceInviteeNotFoundException
import com.docgraph.backend.workspace.command.domain.WorkspaceMemberDuplicateException
import com.docgraph.backend.workspace.command.domain.WorkspaceMemberNotFoundException
import com.docgraph.backend.workspace.command.domain.WorkspaceNotFoundException
import com.docgraph.backend.workspace.command.domain.WorkspacePermissionDeniedException
import com.docgraph.backend.workspace.command.domain.WorkspaceSelfInviteException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/workspaces")
@Tag(name = "Workspace")
class WorkspaceCommandController(
    private val inviteHandler: InviteWorkspaceMemberCommandHandler,
    private val removeHandler: RemoveWorkspaceMemberCommandHandler,
    private val getCurrentUserId: GetCurrentUserIdQuery,
) {

    @PostMapping("/{id}/members")
    @Operation(
        summary = "멤버 초대",
        description = "워크스페이스 멤버로 초대한다. 가입 사용자만 이메일로 검색해 추가. 초대된 멤버는 프로젝트 배정 후보군이 되며, 개별 프로젝트에 배정되기 전까지는 어떤 프로젝트에도 접근할 수 없다.",
        responses = [
            ApiResponse(responseCode = "200", description = "초대 완료 — workspace_member row id 반환"),
            ApiResponse(responseCode = "403", description = "호출자가 워크스페이스 생성자 아님"),
            ApiResponse(responseCode = "404", description = "워크스페이스 없음 또는 invitee 이메일이 가입 사용자에 매칭 안 됨"),
            ApiResponse(responseCode = "409", description = "자기 자신 초대 또는 이미 멤버"),
        ],
    )
    fun inviteMember(
        @PathVariable id: Long,
        @RequestBody request: InviteMemberRequest,
    ): ResponseEntity<IdResponse> {
        val membershipId = inviteHandler.handle(
            InviteWorkspaceMemberCommand(
                workspaceId = id,
                requesterUserId = getCurrentUserId.get(),
                inviteeEmail = request.email,
            ),
        )
        return ResponseEntity.ok(IdResponse(membershipId))
    }

    @DeleteMapping("/{id}/members/{userId}")
    @Operation(summary = "멤버 제거")
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "제거 완료"),
        ApiResponse(responseCode = "400", description = "생성자 본인 제거 시도 — 생성자는 워크스페이스 멤버 row 자체가 아니라 제거 대상 X"),
        ApiResponse(responseCode = "403", description = "호출자가 워크스페이스 생성자 아님"),
        ApiResponse(responseCode = "404", description = "워크스페이스 없음 또는 해당 사용자가 워크스페이스 멤버 아님"),
    ])
    fun removeMember(
        @PathVariable id: Long,
        @PathVariable userId: Long,
    ): ResponseEntity<Unit> {
        removeHandler.handle(
            RemoveWorkspaceMemberCommand(
                workspaceId = id,
                requesterUserId = getCurrentUserId.get(),
                userId = userId,
            ),
        )
        return ResponseEntity.noContent().build()
    }

    @ExceptionHandler(
        WorkspaceNotFoundException::class,
        WorkspaceInviteeNotFoundException::class,
        WorkspaceMemberNotFoundException::class,
    )
    fun handleNotFound(): ResponseEntity<Unit> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).build()

    @ExceptionHandler(WorkspacePermissionDeniedException::class)
    fun handleForbidden(): ResponseEntity<Unit> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).build()

    @ExceptionHandler(
        WorkspaceMemberDuplicateException::class,
        WorkspaceSelfInviteException::class,
    )
    fun handleConflict(): ResponseEntity<Unit> =
        ResponseEntity.status(HttpStatus.CONFLICT).build()

    @ExceptionHandler(WorkspaceCreatorRemovalException::class)
    fun handleBadRequest(): ResponseEntity<Unit> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
}

data class InviteMemberRequest(
    @Schema(description = "초대할 가입 사용자 이메일", example = "user@example.com")
    val email: String,
)
