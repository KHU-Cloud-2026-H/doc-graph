package com.docgraph.backend.workspace.query.interfaces.api

import com.docgraph.backend.workspace.query.application.WorkspaceDetail
import com.docgraph.backend.workspace.query.application.WorkspaceSummary
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/workspaces")
@Tag(name = "Workspace")
class WorkspaceQueryController {

    @GetMapping
    @Operation(summary = "내 워크스페이스 목록")
    fun list(): ResponseEntity<List<WorkspaceSummary>> {
        TODO()
    }

    @GetMapping("/{id}")
    @Operation(summary = "워크스페이스 상세")
    fun get(@PathVariable id: Long): ResponseEntity<WorkspaceDetail> {
        TODO()
    }
}
