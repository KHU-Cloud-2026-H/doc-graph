package com.docgraph.backend.workspace.command.interfaces.api

import com.docgraph.backend.common.IdResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/workspaces")
@Tag(name = "Workspace")
class WorkspaceCommandController {

    @PostMapping
    @Operation(summary = "워크스페이스 생성")
    fun create(@RequestBody request: CreateWorkspaceRequest): ResponseEntity<IdResponse> {
        TODO()
    }

    @PostMapping("/{id}/members")
    @Operation(
        summary = "멤버 초대",
        description = "워크스페이스 멤버로 초대한다. 초대된 멤버는 프로젝트 배정 후보군이 되며, 개별 프로젝트에 배정되기 전까지는 어떤 프로젝트에도 접근할 수 없다.",
    )
    fun inviteMember(
        @PathVariable id: Long,
        @RequestBody request: InviteMemberRequest,
    ): ResponseEntity<IdResponse> {
        TODO()
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @Operation(summary = "멤버 제거")
    fun removeMember(
        @PathVariable id: Long,
        @PathVariable memberId: Long,
    ): ResponseEntity<Unit> {
        TODO()
    }
}

data class CreateWorkspaceRequest(
    @Schema(description = "워크스페이스 이름", example = "My Team")
    val name: String,
)

data class InviteMemberRequest(
    @Schema(description = "초대할 멤버 이메일", example = "member@example.com")
    val email: String,
)
