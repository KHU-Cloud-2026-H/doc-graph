package com.docgraph.backend.project.query.interfaces.api

import com.docgraph.backend.auth.query.application.GetCurrentUserIdQuery
import com.docgraph.backend.project.query.application.CategoryResponse
import com.docgraph.backend.project.query.application.FindProjectDetailByIdQuery
import com.docgraph.backend.project.query.application.ProjectDetail
import com.docgraph.backend.project.query.application.ProjectSummary
import com.docgraph.backend.project.query.application.SearchAccessibleProjectsByWorkspaceQuery
import com.docgraph.backend.project.query.application.SearchCategoryDetailsByProjectQuery
import com.docgraph.backend.project.query.application.SearchTypeAssigneeDefaultsByProjectQuery
import com.docgraph.backend.project.query.application.TypeAssigneeResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Project")
class ProjectQueryController(
    private val searchProjectsByWorkspace: SearchAccessibleProjectsByWorkspaceQuery,
    private val findProjectDetailById: FindProjectDetailByIdQuery,
    private val searchCategoryDetailsByProject: SearchCategoryDetailsByProjectQuery,
    private val searchTypeAssigneesByProject: SearchTypeAssigneeDefaultsByProjectQuery,
    private val getCurrentUserId: GetCurrentUserIdQuery,
) {

    @GetMapping("/workspaces/{workspaceId}/projects")
    @Operation(summary = "프로젝트 목록", description = "본인이 ProjectMember로 배정된 프로젝트만 반환. 비멤버는 빈 list.")
    fun list(@PathVariable workspaceId: Long): ResponseEntity<List<ProjectSummary>> =
        ResponseEntity.ok(searchProjectsByWorkspace.search(workspaceId, getCurrentUserId.get()))

    @GetMapping("/projects/{id}")
    @Operation(summary = "프로젝트 상세")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "조회 완료"),
        ApiResponse(responseCode = "404", description = "프로젝트 없음 또는 본인 비멤버", content = [Content(schema = Schema(implementation = Void::class))]),
    ])
    fun get(@PathVariable id: Long): ResponseEntity<ProjectDetail> {
        val detail = findProjectDetailById.find(id, getCurrentUserId.get())
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(detail)
    }

    @GetMapping("/projects/{id}/categories")
    @Operation(summary = "카테고리 목록")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "조회 완료"),
        ApiResponse(responseCode = "404", description = "프로젝트 없음 또는 본인 비멤버", content = [Content(schema = Schema(implementation = Void::class))]),
    ])
    fun getCategories(@PathVariable id: Long): ResponseEntity<List<CategoryResponse>> {
        val list = searchCategoryDetailsByProject.search(id, getCurrentUserId.get())
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(list)
    }

    @GetMapping("/projects/{id}/type-assignees")
    @Operation(summary = "타입별 담당자 기본값 조회")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "조회 완료"),
        ApiResponse(responseCode = "404", description = "프로젝트 없음 또는 본인 비멤버", content = [Content(schema = Schema(implementation = Void::class))]),
    ])
    fun getTypeAssignees(@PathVariable id: Long): ResponseEntity<List<TypeAssigneeResponse>> {
        val list = searchTypeAssigneesByProject.search(id, getCurrentUserId.get())
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(list)
    }
}
