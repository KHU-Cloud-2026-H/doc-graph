package com.docgraph.backend.project.command.interfaces.api

import com.docgraph.backend.auth.query.application.GetCurrentUserIdQuery
import com.docgraph.backend.document.query.application.DocumentType
import com.docgraph.backend.project.command.application.AssignProjectMemberCommand
import com.docgraph.backend.project.command.application.AssignProjectMemberCommandHandler
import com.docgraph.backend.project.command.application.ChangeCategoryTypeCommand
import com.docgraph.backend.project.command.application.ChangeCategoryTypeCommandHandler
import com.docgraph.backend.project.command.application.ChangeProjectMemberRoleCommand
import com.docgraph.backend.project.command.application.ChangeProjectMemberRoleCommandHandler
import com.docgraph.backend.project.command.application.ConfigureTypeAssigneesCommand
import com.docgraph.backend.project.command.application.ConfigureTypeAssigneesCommandHandler
import com.docgraph.backend.project.command.application.TypeAssigneeAssignment
import com.docgraph.backend.project.command.application.DiscardProjectCommand
import com.docgraph.backend.project.command.application.DiscardProjectCommandHandler
import com.docgraph.backend.project.command.application.RegisterCategoryCommand
import com.docgraph.backend.project.command.application.RegisterCategoryCommandHandler
import com.docgraph.backend.project.command.application.RegisterProjectCommand
import com.docgraph.backend.project.command.application.RegisterProjectCommandHandler
import com.docgraph.backend.project.command.application.RemoveCategoryCommand
import com.docgraph.backend.project.command.application.RemoveCategoryCommandHandler
import com.docgraph.backend.project.command.application.RemoveProjectMemberCommand
import com.docgraph.backend.project.command.application.RemoveProjectMemberCommandHandler
import com.docgraph.backend.project.command.application.TriggerProjectSyncCommand
import com.docgraph.backend.project.command.application.TriggerProjectSyncCommandHandler
import com.docgraph.backend.project.command.domain.CategoryDuplicateException
import com.docgraph.backend.project.command.domain.CategoryNotFoundException
import com.docgraph.backend.project.command.domain.ProjectDuplicateException
import com.docgraph.backend.project.command.domain.ProjectMemberDuplicateException
import com.docgraph.backend.project.command.domain.ProjectMemberNotFoundException
import com.docgraph.backend.project.command.domain.ProjectMemberRole
import com.docgraph.backend.project.command.domain.ProjectNotFoundException
import com.docgraph.backend.project.command.domain.ProjectPermissionDeniedException
import com.docgraph.backend.project.command.domain.ProjectSelfMemberModificationException
import com.docgraph.backend.project.command.domain.ProjectWorkspaceMemberNotFoundException
import com.docgraph.backend.project.command.domain.ProjectWorkspaceNotFoundException
import com.docgraph.backend.web.IdResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@Tag(name = "Project")
class ProjectCommandController(
    private val registerHandler: RegisterProjectCommandHandler,
    private val discardHandler: DiscardProjectCommandHandler,
    private val assignMemberHandler: AssignProjectMemberCommandHandler,
    private val changeMemberRoleHandler: ChangeProjectMemberRoleCommandHandler,
    private val removeMemberHandler: RemoveProjectMemberCommandHandler,
    private val registerCategoryHandler: RegisterCategoryCommandHandler,
    private val changeCategoryTypeHandler: ChangeCategoryTypeCommandHandler,
    private val removeCategoryHandler: RemoveCategoryCommandHandler,
    private val configureTypeAssigneesHandler: ConfigureTypeAssigneesCommandHandler,
    private val triggerSyncHandler: TriggerProjectSyncCommandHandler,
    private val getCurrentUserId: GetCurrentUserIdQuery,
) {

    @PostMapping("/workspaces/{workspaceId}/projects")
    @Operation(
        summary = "프로젝트 생성",
        description = "Notion 루트 페이지를 기준으로 프로젝트를 생성한다. 생성 직후 동기화는 실행되지 않는다. 카테고리 등록·담당자 기본값·멤버 배정을 완료한 후 POST /projects/{id}/sync로 초기 동기화를 수동 트리거해야 한다.",
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "생성 완료 — project row id 반환"),
        ApiResponse(responseCode = "403", description = "호출자가 워크스페이스 생성자 아님", content = [Content(schema = Schema(implementation = Void::class))]),
        ApiResponse(responseCode = "404", description = "워크스페이스 없음", content = [Content(schema = Schema(implementation = Void::class))]),
        ApiResponse(responseCode = "409", description = "같은 워크스페이스에 같은 루트 페이지 프로젝트 이미 존재", content = [Content(schema = Schema(implementation = Void::class))]),
    ])
    fun create(
        @PathVariable workspaceId: Long,
        @RequestBody request: CreateProjectRequest,
    ): ResponseEntity<IdResponse> {
        val projectId = registerHandler.handle(
            RegisterProjectCommand(
                workspaceId = workspaceId,
                requesterUserId = getCurrentUserId.get(),
                name = request.name,
                notionRootPageId = request.notionRootPageId,
            ),
        )
        return ResponseEntity.ok(IdResponse(projectId))
    }

    @DeleteMapping("/projects/{id}")
    @Operation(summary = "프로젝트 삭제")
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "삭제 완료"),
        ApiResponse(responseCode = "403", description = "호출자가 Project Admin 아님", content = [Content(schema = Schema(implementation = Void::class))]),
        ApiResponse(responseCode = "404", description = "프로젝트 없음", content = [Content(schema = Schema(implementation = Void::class))]),
    ])
    fun delete(@PathVariable id: Long): ResponseEntity<Unit> {
        discardHandler.handle(
            DiscardProjectCommand(
                projectId = id,
                requesterUserId = getCurrentUserId.get(),
            ),
        )
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/projects/{id}/members")
    @Operation(summary = "프로젝트 멤버 배정")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "배정 완료 — project_member row id 반환"),
        ApiResponse(responseCode = "403", description = "호출자가 Project Admin 아님", content = [Content(schema = Schema(implementation = Void::class))]),
        ApiResponse(responseCode = "404", description = "프로젝트 없음 또는 workspace_member가 프로젝트의 워크스페이스에 속하지 않음", content = [Content(schema = Schema(implementation = Void::class))]),
        ApiResponse(responseCode = "409", description = "이미 프로젝트 멤버", content = [Content(schema = Schema(implementation = Void::class))]),
    ])
    fun assignMember(
        @PathVariable id: Long,
        @RequestBody request: AssignMemberRequest,
    ): ResponseEntity<IdResponse> {
        val memberId = assignMemberHandler.handle(
            AssignProjectMemberCommand(
                projectId = id,
                requesterUserId = getCurrentUserId.get(),
                workspaceMemberId = request.workspaceMemberId,
                role = request.role,
            ),
        )
        return ResponseEntity.ok(IdResponse(memberId))
    }

    @PatchMapping("/projects/{id}/members/{memberId}")
    @Operation(summary = "프로젝트 멤버 역할 변경")
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "변경 완료"),
        ApiResponse(responseCode = "400", description = "자기 자신 역할 변경 시도", content = [Content(schema = Schema(implementation = Void::class))]),
        ApiResponse(responseCode = "403", description = "호출자가 Project Admin 아님", content = [Content(schema = Schema(implementation = Void::class))]),
        ApiResponse(responseCode = "404", description = "프로젝트 없음 또는 project_member 없음", content = [Content(schema = Schema(implementation = Void::class))]),
    ])
    fun updateMemberRole(
        @PathVariable id: Long,
        @PathVariable memberId: Long,
        @RequestBody request: UpdateProjectMemberRoleRequest,
    ): ResponseEntity<Unit> {
        changeMemberRoleHandler.handle(
            ChangeProjectMemberRoleCommand(
                projectId = id,
                requesterUserId = getCurrentUserId.get(),
                memberId = memberId,
                role = request.role,
            ),
        )
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/projects/{id}/members/{memberId}")
    @Operation(summary = "프로젝트 멤버 제거")
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "제거 완료"),
        ApiResponse(responseCode = "400", description = "자기 자신 제거 시도", content = [Content(schema = Schema(implementation = Void::class))]),
        ApiResponse(responseCode = "403", description = "호출자가 Project Admin 아님", content = [Content(schema = Schema(implementation = Void::class))]),
        ApiResponse(responseCode = "404", description = "프로젝트 없음 또는 project_member 없음", content = [Content(schema = Schema(implementation = Void::class))]),
    ])
    fun removeMember(
        @PathVariable id: Long,
        @PathVariable memberId: Long,
    ): ResponseEntity<Unit> {
        removeMemberHandler.handle(
            RemoveProjectMemberCommand(
                projectId = id,
                requesterUserId = getCurrentUserId.get(),
                memberId = memberId,
            ),
        )
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/projects/{id}/categories")
    @Operation(
        summary = "카테고리 등록",
        description = "프로젝트 루트 페이지의 직계 자식 페이지를 카테고리로 등록하고 문서 타입을 매핑한다. 문서는 자기 조상 라인의 카테고리 매핑을 조회해 타입을 결정한다.",
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "등록 완료 — category row id 반환"),
        ApiResponse(responseCode = "403", description = "호출자가 Project Admin 아님", content = [Content(schema = Schema(implementation = Void::class))]),
        ApiResponse(responseCode = "404", description = "프로젝트 없음", content = [Content(schema = Schema(implementation = Void::class))]),
        ApiResponse(responseCode = "409", description = "같은 notion_page_id 카테고리 이미 존재", content = [Content(schema = Schema(implementation = Void::class))]),
    ])
    fun registerCategory(
        @PathVariable id: Long,
        @RequestBody request: RegisterCategoryRequest,
    ): ResponseEntity<IdResponse> {
        val categoryId = registerCategoryHandler.handle(
            RegisterCategoryCommand(
                projectId = id,
                requesterUserId = getCurrentUserId.get(),
                notionPageId = request.notionPageId,
                documentType = request.documentType,
            ),
        )
        return ResponseEntity.ok(IdResponse(categoryId))
    }

    @PatchMapping("/projects/{id}/categories/{categoryId}")
    @Operation(summary = "카테고리 타입 변경")
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "변경 완료"),
        ApiResponse(responseCode = "403", description = "호출자가 Project Admin 아님", content = [Content(schema = Schema(implementation = Void::class))]),
        ApiResponse(responseCode = "404", description = "프로젝트 또는 카테고리 없음", content = [Content(schema = Schema(implementation = Void::class))]),
    ])
    fun changeCategoryType(
        @PathVariable id: Long,
        @PathVariable categoryId: Long,
        @RequestBody request: ChangeCategoryTypeRequest,
    ): ResponseEntity<Unit> {
        changeCategoryTypeHandler.handle(
            ChangeCategoryTypeCommand(
                projectId = id,
                requesterUserId = getCurrentUserId.get(),
                categoryId = categoryId,
                documentType = request.documentType,
            ),
        )
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/projects/{id}/categories/{categoryId}")
    @Operation(summary = "카테고리 제거")
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "제거 완료"),
        ApiResponse(responseCode = "403", description = "호출자가 Project Admin 아님", content = [Content(schema = Schema(implementation = Void::class))]),
        ApiResponse(responseCode = "404", description = "프로젝트 또는 카테고리 없음", content = [Content(schema = Schema(implementation = Void::class))]),
    ])
    fun removeCategory(
        @PathVariable id: Long,
        @PathVariable categoryId: Long,
    ): ResponseEntity<Unit> {
        removeCategoryHandler.handle(
            RemoveCategoryCommand(
                projectId = id,
                requesterUserId = getCurrentUserId.get(),
                categoryId = categoryId,
            ),
        )
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/projects/{id}/type-assignees")
    @Operation(
        summary = "타입별 담당자 기본값 수정",
        description = "문서 타입별 기본 담당자를 전체 replace 의미로 설정한다. assigneeMemberId는 워크스페이스 멤버 ID이며, null이면 담당자 없음을 의미한다. 빈 list는 전체 row 삭제.",
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "설정 완료"),
        ApiResponse(responseCode = "403", description = "호출자가 Project Admin 아님", content = [Content(schema = Schema(implementation = Void::class))]),
        ApiResponse(responseCode = "404", description = "프로젝트 없음 또는 assignee가 프로젝트의 워크스페이스에 속하지 않음", content = [Content(schema = Schema(implementation = Void::class))]),
    ])
    fun updateTypeAssignees(
        @PathVariable id: Long,
        @RequestBody request: UpdateTypeAssigneesRequest,
    ): ResponseEntity<Unit> {
        configureTypeAssigneesHandler.handle(
            ConfigureTypeAssigneesCommand(
                projectId = id,
                requesterUserId = getCurrentUserId.get(),
                assignments = request.assignees.map {
                    TypeAssigneeAssignment(documentType = it.documentType, assigneeWorkspaceMemberId = it.assigneeMemberId)
                },
            ),
        )
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/projects/{id}/sync")
    @Operation(
        summary = "수동 동기화 트리거",
        description = "Notion 루트 페이지 하위 트리를 전체 조회해 문서 스냅샷을 갱신하고, 엣지·연결 제안 생성을 시작한다. 프로젝트 생성 후 카테고리·담당자 기본값·멤버 배정이 끝난 뒤 호출한다. ProjectSyncTriggeredEvent 발행만 — document worker가 listen.",
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "트리거 완료 (이벤트 발행)"),
        ApiResponse(responseCode = "403", description = "호출자가 Project Admin 아님", content = [Content(schema = Schema(implementation = Void::class))]),
        ApiResponse(responseCode = "404", description = "프로젝트 없음", content = [Content(schema = Schema(implementation = Void::class))]),
    ])
    fun sync(@PathVariable id: Long): ResponseEntity<Unit> {
        triggerSyncHandler.handle(
            TriggerProjectSyncCommand(
                projectId = id,
                requesterUserId = getCurrentUserId.get(),
            ),
        )
        return ResponseEntity.noContent().build()
    }

    @ExceptionHandler(
        ProjectNotFoundException::class,
        ProjectWorkspaceNotFoundException::class,
        ProjectMemberNotFoundException::class,
        ProjectWorkspaceMemberNotFoundException::class,
        CategoryNotFoundException::class,
    )
    fun handleNotFound(): ResponseEntity<Unit> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).build()

    @ExceptionHandler(ProjectPermissionDeniedException::class)
    fun handleForbidden(): ResponseEntity<Unit> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).build()

    @ExceptionHandler(
        ProjectDuplicateException::class,
        ProjectMemberDuplicateException::class,
        CategoryDuplicateException::class,
    )
    fun handleConflict(): ResponseEntity<Unit> =
        ResponseEntity.status(HttpStatus.CONFLICT).build()

    @ExceptionHandler(ProjectSelfMemberModificationException::class)
    fun handleBadRequest(): ResponseEntity<Unit> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
}

data class CreateProjectRequest(
    @Schema(description = "프로젝트 이름", example = "Q2 Planning")
    val name: String,
    @Schema(description = "Notion 루트 페이지 ID", example = "abc1234567890def")
    val notionRootPageId: String,
)

data class AssignMemberRequest(
    @Schema(description = "배정할 워크스페이스 멤버 ID", example = "1")
    val workspaceMemberId: Long,
    @Schema(description = "역할")
    val role: ProjectMemberRole,
)

data class UpdateProjectMemberRoleRequest(
    @Schema(description = "변경할 역할")
    val role: ProjectMemberRole,
)

data class RegisterCategoryRequest(
    @Schema(description = "Notion 페이지 ID", example = "abc1234567890def")
    val notionPageId: String,
    @Schema(description = "문서 타입")
    val documentType: DocumentType,
)

data class ChangeCategoryTypeRequest(
    @Schema(description = "변경할 문서 타입")
    val documentType: DocumentType,
)

data class UpdateTypeAssigneesRequest(
    @Schema(description = "타입별 담당자 기본값 목록")
    val assignees: List<TypeAssigneeItem>,
)

data class TypeAssigneeItem(
    @Schema(description = "문서 타입")
    val documentType: DocumentType,
    @Schema(description = "담당자 워크스페이스 멤버 ID (null이면 담당자 없음)", example = "1")
    val assigneeMemberId: Long?,
)
