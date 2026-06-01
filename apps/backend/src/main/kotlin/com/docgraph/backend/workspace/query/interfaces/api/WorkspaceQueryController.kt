package com.docgraph.backend.workspace.query.interfaces.api

import com.docgraph.backend.auth.query.application.GetCurrentUserIdQuery
import com.docgraph.backend.workspace.query.application.FindWorkspaceDetailByIdQuery
import com.docgraph.backend.workspace.query.application.FindNotionWorkspacePageContentQuery
import com.docgraph.backend.workspace.query.application.FindNotionPageMetadataQuery
import com.docgraph.backend.workspace.query.application.NotionWorkspacePageContentResponse
import com.docgraph.backend.workspace.query.application.NotionWorkspacePageMetadataResponse
import com.docgraph.backend.workspace.query.application.NotionWorkspacePageResponse
import com.docgraph.backend.workspace.query.application.SearchAccessibleWorkspacesQuery
import com.docgraph.backend.workspace.query.application.SearchNotionPageChildrenQuery
import com.docgraph.backend.workspace.query.application.SearchNotionRootPagesQuery
import com.docgraph.backend.workspace.query.application.SearchNotionWorkspacePagesQuery
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/workspaces")
@Tag(name = "Workspace")
class WorkspaceQueryController(
    private val searchAccessibleWorkspaces: SearchAccessibleWorkspacesQuery,
    private val findWorkspaceDetailById: FindWorkspaceDetailByIdQuery,
    private val searchNotionWorkspacePages: SearchNotionWorkspacePagesQuery,
    private val searchNotionRootPages: SearchNotionRootPagesQuery,
    private val searchNotionPageChildren: SearchNotionPageChildrenQuery,
    private val findNotionPageMetadata: FindNotionPageMetadataQuery,
    private val findNotionWorkspacePageContent: FindNotionWorkspacePageContentQuery,
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

    @GetMapping("/{id}/notion/pages")
    @Operation(summary = "Notion 워크스페이스 접근 가능 페이지 목록")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "OAuth로 연결된 Notion workspace에서 검색 가능한 page 목록"),
        ApiResponse(responseCode = "404", description = "워크스페이스 없음 또는 접근 권한/Notion 연결 없음"),
    ])
    fun listNotionPages(
        @PathVariable id: Long,
        @RequestParam(required = false) query: String?,
    ): ResponseEntity<List<NotionWorkspacePageResponse>> {
        val pages = searchNotionWorkspacePages.search(id, getCurrentUserId.get(), query)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        return ResponseEntity.ok(pages)
    }

    @GetMapping("/{id}/notion/root-pages")
    @Operation(summary = "Notion 루트 페이지 후보 목록")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "새 프로젝트 생성 시 선택 가능한 최상위 page 목록"),
        ApiResponse(responseCode = "404", description = "워크스페이스 없음 또는 접근 권한/Notion 연결 없음"),
    ])
    fun listNotionRootPages(
        @PathVariable id: Long,
    ): ResponseEntity<List<NotionWorkspacePageResponse>> {
        val pages = searchNotionRootPages.searchRootPages(id, getCurrentUserId.get())
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        return ResponseEntity.ok(pages)
    }

    @GetMapping("/{id}/notion/pages/{pageId}/children")
    @Operation(summary = "Notion page 직계 자식 page 목록")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "카테고리 매핑에 사용할 루트 page 직계 자식 page 목록"),
        ApiResponse(responseCode = "404", description = "워크스페이스 없음 또는 접근 권한/Notion 연결/page 접근 없음"),
    ])
    fun listNotionPageChildren(
        @PathVariable id: Long,
        @PathVariable pageId: String,
    ): ResponseEntity<List<NotionWorkspacePageResponse>> {
        val pages = searchNotionPageChildren.searchPageChildren(id, getCurrentUserId.get(), pageId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        return ResponseEntity.ok(pages)
    }

    @GetMapping("/{id}/notion/pages/{pageId}/metadata")
    @Operation(summary = "Notion page 제목/아이콘 조회")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Notion page 제목과 아이콘"),
        ApiResponse(responseCode = "404", description = "워크스페이스 없음 또는 접근 권한/Notion 연결/page 접근 없음"),
    ])
    fun getNotionPageMetadata(
        @PathVariable id: Long,
        @PathVariable pageId: String,
    ): ResponseEntity<NotionWorkspacePageMetadataResponse> {
        val metadata = findNotionPageMetadata.find(id, getCurrentUserId.get(), pageId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        return ResponseEntity.ok(metadata)
    }

    @GetMapping("/{id}/notion/pages/{pageId}/content")
    @Operation(summary = "Notion page 본문 block/text 조회")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "OAuth로 연결된 Notion page의 block tree와 flat text"),
        ApiResponse(responseCode = "404", description = "워크스페이스 없음 또는 접근 권한/Notion 연결/page 접근 없음"),
    ])
    fun getNotionPageContent(
        @PathVariable id: Long,
        @PathVariable pageId: String,
    ): ResponseEntity<NotionWorkspacePageContentResponse> {
        val content = findNotionWorkspacePageContent.find(id, getCurrentUserId.get(), pageId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        return ResponseEntity.ok(content)
    }
}
