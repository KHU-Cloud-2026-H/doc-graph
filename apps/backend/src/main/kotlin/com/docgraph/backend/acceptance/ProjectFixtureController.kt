package com.docgraph.backend.acceptance

import com.docgraph.backend.auth.query.application.GetCurrentUserIdQuery
import com.docgraph.backend.project.command.domain.Project
import com.docgraph.backend.project.command.domain.ProjectMember
import com.docgraph.backend.project.command.domain.ProjectMemberRepository
import com.docgraph.backend.project.command.domain.ProjectMemberRole
import com.docgraph.backend.project.command.domain.ProjectRepository
import com.docgraph.backend.web.IdResponse
import com.docgraph.backend.workspace.command.domain.WorkspaceMemberRepository
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * acceptance profile 한정 — UC spec이 검증 대상 외 project 상태를 seed하는 우회 진입점.
 * 운영 흐름은 워크스페이스 생성자가 `POST /workspaces/{id}/projects` 호출.
 *
 * RegisterProjectCommandHandler invariant(workspace 생성자 검증 + ProjectMember Admin row 동시 insert)를
 * 영속 직접 호출로 우회. 호출자(`X-Test-User-Id`)의 workspace_member row가 있으면 ProjectMember Admin 동시 박기.
 */
@RestController
@Profile("acceptance")
class ProjectFixtureController(
    private val projectRepository: ProjectRepository,
    private val projectMemberRepository: ProjectMemberRepository,
    private val workspaceMemberRepository: WorkspaceMemberRepository,
    private val getCurrentUserId: GetCurrentUserIdQuery,
) {

    @PostMapping("/test/projects")
    @Transactional
    fun seedProject(@RequestBody request: SeedProjectRequest): ResponseEntity<IdResponse> {
        val creatorUserId = getCurrentUserId.get()
        val project = projectRepository.save(
            Project(
                workspaceId = request.workspaceId,
                name = request.name,
                notionRootPageId = request.notionRootPageId ?: "root-fixture-${System.nanoTime()}",
                createdBy = creatorUserId,
            ),
        )
        val wsMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(request.workspaceId, creatorUserId)
        if (wsMember != null) {
            projectMemberRepository.save(
                ProjectMember(
                    projectId = project.id,
                    workspaceMemberId = wsMember.id,
                    role = ProjectMemberRole.ADMIN,
                ),
            )
        }
        return ResponseEntity.ok(IdResponse(project.id))
    }

    @PostMapping("/test/project-members")
    @Transactional
    fun seedProjectMember(@RequestBody request: SeedProjectMemberRequest): ResponseEntity<IdResponse> {
        val saved = projectMemberRepository.save(
            ProjectMember(
                projectId = request.projectId,
                workspaceMemberId = request.workspaceMemberId,
                role = request.role,
            ),
        )
        return ResponseEntity.ok(IdResponse(saved.id))
    }
}

data class SeedProjectRequest(
    @Schema(example = "1")
    val workspaceId: Long,
    @Schema(example = "Q2 Planning")
    val name: String,
    @Schema(description = "지정 시 명시 페이지 ID 사용, 없으면 fixture가 자동 생성", example = "abc1234567890def")
    val notionRootPageId: String? = null,
)

data class SeedProjectMemberRequest(
    @Schema(example = "1")
    val projectId: Long,
    @Schema(example = "1")
    val workspaceMemberId: Long,
    @Schema(description = "역할")
    val role: ProjectMemberRole = ProjectMemberRole.MEMBER,
)
