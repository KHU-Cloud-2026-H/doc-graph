package com.docgraph.backend.workspace.query.interfaces.api

import com.docgraph.backend.auth.query.application.GetCurrentUserIdQuery
import com.docgraph.backend.workspace.query.application.FindWorkspaceDetailByIdQuery
import com.docgraph.backend.workspace.query.application.SearchAccessibleWorkspacesQuery
import com.docgraph.backend.workspace.query.application.WorkspaceDetail
import com.docgraph.backend.workspace.query.application.WorkspaceSummary
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/workspaces")
@Tag(name = "Workspace")
class WorkspaceQueryController(
    private val searchAccessibleWorkspaces: SearchAccessibleWorkspacesQuery,
    private val findWorkspaceDetailById: FindWorkspaceDetailByIdQuery,
    private val getCurrentUserId: GetCurrentUserIdQuery,
) {

    @GetMapping
    @Operation(summary = "내 워크스페이스 목록 — 소유 + 멤버 참여 모두")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "워크스페이스 목록 (접근 가능한 것 없으면 빈 배열)"),
    ])
    fun list(): ResponseEntity<List<WorkspaceSummary>> =
        ResponseEntity.ok(searchAccessibleWorkspaces.search(getCurrentUserId.get()))

    @GetMapping("/{id}")
    @Operation(summary = "워크스페이스 상세")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "워크스페이스 상세 (멤버 목록 포함)"),
        ApiResponse(responseCode = "404", description = "워크스페이스 없음 또는 접근 권한 없음 (구분 X — security 측면에서 동일 응답)"),
    ])
    fun get(@PathVariable id: Long): ResponseEntity<WorkspaceDetail> {
        val detail = findWorkspaceDetailById.find(id, getCurrentUserId.get())
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        return ResponseEntity.ok(detail)
    }
}
