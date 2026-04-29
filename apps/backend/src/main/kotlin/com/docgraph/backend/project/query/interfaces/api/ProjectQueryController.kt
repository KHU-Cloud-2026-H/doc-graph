package com.docgraph.backend.project.query.interfaces.api

import com.docgraph.backend.project.query.application.ProjectDetail
import com.docgraph.backend.project.query.application.ProjectSummary
import com.docgraph.backend.project.query.application.TypeAssigneeResponse
import com.docgraph.backend.project.query.application.TypeMappingResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@Tag(name = "Project")
class ProjectQueryController {

    @GetMapping("/workspaces/{workspaceId}/projects")
    @Operation(summary = "프로젝트 목록")
    fun list(@PathVariable workspaceId: Long): ResponseEntity<List<ProjectSummary>> {
        TODO()
    }

    @GetMapping("/projects/{id}")
    @Operation(summary = "프로젝트 상세")
    fun get(@PathVariable id: Long): ResponseEntity<ProjectDetail> {
        TODO()
    }

    @GetMapping("/projects/{id}/type-mappings")
    @Operation(summary = "상위 페이지-타입 매핑 조회")
    fun getTypeMappings(@PathVariable id: Long): ResponseEntity<List<TypeMappingResponse>> {
        TODO()
    }

    @GetMapping("/projects/{id}/type-assignees")
    @Operation(summary = "타입별 담당자 기본값 조회")
    fun getTypeAssignees(@PathVariable id: Long): ResponseEntity<List<TypeAssigneeResponse>> {
        TODO()
    }
}
