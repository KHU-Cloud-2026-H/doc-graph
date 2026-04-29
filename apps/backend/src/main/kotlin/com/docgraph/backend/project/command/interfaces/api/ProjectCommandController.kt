package com.docgraph.backend.project.command.interfaces.api

import com.docgraph.backend.common.DocumentType
import com.docgraph.backend.common.IdResponse
import com.docgraph.backend.common.ProjectMemberRole
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@Tag(name = "Project")
class ProjectCommandController {

    @PostMapping("/workspaces/{workspaceId}/projects")
    @Operation(
        summary = "프로젝트 생성",
        description = "Notion 루트 페이지를 기준으로 프로젝트를 생성한다. 생성 직후 동기화는 실행되지 않는다. 타입 매핑·담당자 기본값·멤버 배정을 완료한 후 POST /projects/{id}/sync로 초기 동기화를 수동 트리거해야 한다.",
    )
    fun create(
        @PathVariable workspaceId: Long,
        @RequestBody request: CreateProjectRequest,
    ): ResponseEntity<IdResponse> {
        TODO()
    }

    @DeleteMapping("/projects/{id}")
    @Operation(summary = "프로젝트 삭제")
    fun delete(@PathVariable id: Long): ResponseEntity<Unit> {
        TODO()
    }

    @PostMapping("/projects/{id}/members")
    @Operation(summary = "프로젝트 멤버 배정")
    fun assignMember(
        @PathVariable id: Long,
        @RequestBody request: AssignMemberRequest,
    ): ResponseEntity<IdResponse> {
        TODO()
    }

    @PatchMapping("/projects/{id}/members/{memberId}")
    @Operation(summary = "프로젝트 멤버 역할 변경")
    fun updateMemberRole(
        @PathVariable id: Long,
        @PathVariable memberId: Long,
        @RequestBody request: UpdateProjectMemberRoleRequest,
    ): ResponseEntity<Unit> {
        TODO()
    }

    @DeleteMapping("/projects/{id}/members/{memberId}")
    @Operation(summary = "프로젝트 멤버 제거")
    fun removeMember(
        @PathVariable id: Long,
        @PathVariable memberId: Long,
    ): ResponseEntity<Unit> {
        TODO()
    }

    @PutMapping("/projects/{id}/type-mappings")
    @Operation(summary = "상위 페이지-타입 매핑 수정")
    fun updateTypeMappings(
        @PathVariable id: Long,
        @RequestBody request: UpdateTypeMappingsRequest,
    ): ResponseEntity<Unit> {
        TODO()
    }

    @PutMapping("/projects/{id}/type-assignees")
    @Operation(
        summary = "타입별 담당자 기본값 수정",
        description = "문서 타입별 기본 담당자를 설정한다. assigneeMemberId는 워크스페이스 멤버 ID이며, null이면 담당자 없음을 의미한다.",
    )
    fun updateTypeAssignees(
        @PathVariable id: Long,
        @RequestBody request: UpdateTypeAssigneesRequest,
    ): ResponseEntity<Unit> {
        TODO()
    }

    @PostMapping("/projects/{id}/sync")
    @Operation(
        summary = "수동 동기화 트리거",
        description = "Notion 루트 페이지 하위 트리를 전체 조회해 문서 스냅샷을 갱신하고, 엣지·연결 제안 생성을 시작한다. 프로젝트 생성 후 타입 매핑·담당자 기본값·멤버 배정이 끝난 뒤 호출한다.",
    )
    fun sync(@PathVariable id: Long): ResponseEntity<Unit> {
        TODO()
    }
}

data class CreateProjectRequest(
    @Schema(description = "프로젝트 이름", example = "Q2 Planning")
    val name: String,
    @Schema(description = "Notion 루트 페이지 ID", example = "abc1234567890def")
    val notionRootPageId: String,
)

data class AssignMemberRequest(
    @Schema(description = "배정할 워크스페이스 멤버 ID", example = "1")
    val memberId: Long,
    @Schema(description = "역할")
    val role: ProjectMemberRole,
)

data class UpdateProjectMemberRoleRequest(
    @Schema(description = "변경할 역할")
    val role: ProjectMemberRole,
)

data class UpdateTypeMappingsRequest(
    @Schema(description = "상위 페이지-타입 매핑 목록")
    val mappings: List<TypeMappingItem>,
)

data class TypeMappingItem(
    @Schema(description = "Notion 페이지 ID", example = "abc1234567890def")
    val notionPageId: String,
    @Schema(description = "문서 타입")
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
