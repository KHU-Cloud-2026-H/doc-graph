package com.docgraph.backend.document.query.interfaces.api

import com.docgraph.backend.auth.query.application.GetCurrentUserIdQuery
import com.docgraph.backend.document.query.application.NotionPageRef
import com.docgraph.backend.document.query.application.SearchNotionChildPagesByParentQuery
import com.docgraph.backend.document.query.application.SearchNotionRootPagesByWorkspaceQuery
import com.docgraph.backend.workspace.query.application.FindWorkspaceCreatorIdByIdQuery
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

/**
 * 프로젝트 생성 위저드의 Notion 페이지 브라우징. 영속된 Document가 아니라 Notion API 라이브 조회다.
 * 워크스페이스 스코프이며 워크스페이스 생성자 전용(프로젝트 생성 권한과 동일 게이트).
 */
@RestController
@Tag(name = "Document")
class NotionPageQueryController(
    private val searchRootPages: SearchNotionRootPagesByWorkspaceQuery,
    private val searchChildPages: SearchNotionChildPagesByParentQuery,
    private val findWorkspaceCreatorId: FindWorkspaceCreatorIdByIdQuery,
    private val getCurrentUserId: GetCurrentUserIdQuery,
) {

    @GetMapping("/workspaces/{workspaceId}/notion-pages")
    @Operation(
        summary = "Notion 최상위 페이지 목록",
        description = "프로젝트 생성 위저드 루트 페이지 후보. 워크스페이스 최상위 Notion 페이지를 라이브 조회한다. 워크스페이스 생성자 전용.",
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "조회 완료"),
        ApiResponse(responseCode = "403", description = "호출자가 워크스페이스 생성자 아님", content = [Content(schema = Schema(implementation = Void::class))]),
        ApiResponse(responseCode = "404", description = "워크스페이스 없음", content = [Content(schema = Schema(implementation = Void::class))]),
    ])
    fun listRootPages(@PathVariable workspaceId: Long): ResponseEntity<List<NotionPageRef>> {
        guardCreator(workspaceId)?.let { return it }
        return ResponseEntity.ok(searchRootPages.search(workspaceId))
    }

    @GetMapping("/workspaces/{workspaceId}/notion-pages/{pageId}/children")
    @Operation(
        summary = "Notion 페이지 직계 자식 목록",
        description = "프로젝트 생성 위저드 카테고리 매핑 대상. 페이지의 직계 자식 Notion 페이지를 라이브 조회한다. 워크스페이스 생성자 전용.",
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "조회 완료"),
        ApiResponse(responseCode = "403", description = "호출자가 워크스페이스 생성자 아님", content = [Content(schema = Schema(implementation = Void::class))]),
        ApiResponse(responseCode = "404", description = "워크스페이스 없음", content = [Content(schema = Schema(implementation = Void::class))]),
    ])
    fun listChildPages(
        @PathVariable workspaceId: Long,
        @PathVariable pageId: String,
    ): ResponseEntity<List<NotionPageRef>> {
        guardCreator(workspaceId)?.let { return it }
        return ResponseEntity.ok(searchChildPages.search(workspaceId, pageId))
    }

    /** 워크스페이스 생성자 가드. 워크스페이스 없음 → 404, 비생성자 → 403, 통과 → null. */
    private fun guardCreator(workspaceId: Long): ResponseEntity<List<NotionPageRef>>? {
        val creatorId = findWorkspaceCreatorId.find(workspaceId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        if (creatorId != getCurrentUserId.get()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        return null
    }
}
