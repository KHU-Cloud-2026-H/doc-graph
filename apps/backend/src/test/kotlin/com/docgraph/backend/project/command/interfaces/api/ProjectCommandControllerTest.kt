package com.docgraph.backend.project.command.interfaces.api

import com.docgraph.backend.auth.query.application.GetCurrentUserIdQuery
import com.docgraph.backend.document.query.application.DocumentType
import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.project.command.domain.Category
import com.docgraph.backend.project.command.domain.CategoryRepository
import com.docgraph.backend.project.command.domain.TypeAssigneeDefault
import com.docgraph.backend.project.command.domain.TypeAssigneeDefaultRepository
import com.docgraph.backend.project.command.domain.Project
import com.docgraph.backend.project.command.domain.ProjectDiscardedEvent
import com.docgraph.backend.project.command.domain.ProjectSyncTriggeredEvent
import com.docgraph.backend.project.command.domain.ProjectMember
import com.docgraph.backend.project.command.domain.ProjectMemberRepository
import com.docgraph.backend.project.command.domain.ProjectMemberRole
import com.docgraph.backend.project.command.domain.ProjectRegisteredEvent
import com.docgraph.backend.project.command.domain.ProjectRepository
import com.docgraph.backend.workspace.command.domain.Workspace
import com.docgraph.backend.workspace.command.domain.WorkspaceMember
import com.docgraph.backend.workspace.command.domain.WorkspaceMemberRepository
import com.docgraph.backend.workspace.command.domain.WorkspaceRepository
import tools.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.context.event.ApplicationEvents
import org.springframework.test.context.event.RecordApplicationEvents
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.OffsetDateTime
import kotlin.test.assertEquals

class FakeGetCurrentUserIdQuery : GetCurrentUserIdQuery {
    @Volatile var userId: Long = 1L
    override fun get(): Long = userId
}

@TestConfiguration
class ProjectCommandControllerTestConfig {
    @Bean @Primary fun fakeGetCurrentUserId() = FakeGetCurrentUserIdQuery()
}

@Tag("component")
@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
@Import(ProjectCommandControllerTestConfig::class, SharedPostgresContainer::class)
class ProjectCommandControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val workspaceRepository: WorkspaceRepository,
    private val workspaceMemberRepository: WorkspaceMemberRepository,
    private val projectRepository: ProjectRepository,
    private val projectMemberRepository: ProjectMemberRepository,
    private val categoryRepository: CategoryRepository,
    private val typeAssigneeRepository: TypeAssigneeDefaultRepository,
    private val getCurrentUserId: FakeGetCurrentUserIdQuery,
) {

    @BeforeEach
    fun reset() {
        getCurrentUserId.userId = 1L
    }

    // ---- Step 1 — Project Register/Discard ----

    @Test
    fun `POST projects — happy, project 생성 + 생성자 Admin row 동시 생성 + 200`() {
        val (workspace, _) = seedWorkspaceWithCreator(creatorUserId = 1L)

        mockMvc.post("/workspaces/${workspace.id}/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CreateProjectRequest(name = "Q2 Planning", notionRootPageId = "page-q2-planning"),
            )
        }.andExpect {
            status { isOk() }
        }

        val projects = projectRepository.findAllByWorkspaceId(workspace.id)
        assertEquals(1, projects.size)
        val project = projects.first()
        assertEquals(workspace.id, project.workspaceId)
        assertEquals("Q2 Planning", project.name)
        assertEquals("page-q2-planning", project.notionRootPageId)
        assertEquals(1L, project.createdBy)

        val members = projectMemberRepository.findAllByProjectId(project.id)
        assertEquals(1, members.size)
        val adminMember = members.first()
        assertEquals(ProjectMemberRole.ADMIN, adminMember.role)
    }

    @Test
    fun `POST projects — validationEnabled 생략 시 ProjectRegisteredEvent enabled=true 발행`(events: ApplicationEvents) {
        val (workspace, _) = seedWorkspaceWithCreator(creatorUserId = 1L)

        mockMvc.post("/workspaces/${workspace.id}/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CreateProjectRequest(name = "Default On", notionRootPageId = "page-default-on"),
            )
        }.andExpect { status { isOk() } }

        val registered = events.stream(ProjectRegisteredEvent::class.java).toList()
        assertEquals(1, registered.size)
        assertEquals(true, registered.first().validationEnabled)
    }

    @Test
    fun `POST projects — validationEnabled=false 시 ProjectRegisteredEvent enabled=false 발행`(events: ApplicationEvents) {
        val (workspace, _) = seedWorkspaceWithCreator(creatorUserId = 1L)

        mockMvc.post("/workspaces/${workspace.id}/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CreateProjectRequest(name = "Start Off", notionRootPageId = "page-start-off", validationEnabled = false),
            )
        }.andExpect { status { isOk() } }

        val registered = events.stream(ProjectRegisteredEvent::class.java).toList()
        assertEquals(1, registered.size)
        assertEquals(false, registered.first().validationEnabled)
    }

    @Test
    fun `POST projects — workspace 없음 → 404`() {
        mockMvc.post("/workspaces/9999999/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CreateProjectRequest(name = "X", notionRootPageId = "page-x"),
            )
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `POST projects — 호출자가 workspace 생성자 아님 → 403`() {
        val (workspace, _) = seedWorkspaceWithCreator(creatorUserId = 1L)
        getCurrentUserId.userId = 999L

        mockMvc.post("/workspaces/${workspace.id}/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CreateProjectRequest(name = "X", notionRootPageId = "page-x"),
            )
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `POST projects — 같은 워크스페이스에 같은 루트 페이지 재생성 → 409`() {
        val (workspace, _) = seedWorkspaceWithCreator(creatorUserId = 1L)

        mockMvc.post("/workspaces/${workspace.id}/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CreateProjectRequest(name = "First", notionRootPageId = "page-dup"),
            )
        }.andExpect { status { isOk() } }

        mockMvc.post("/workspaces/${workspace.id}/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CreateProjectRequest(name = "Second", notionRootPageId = "page-dup"),
            )
        }.andExpect { status { isConflict() } }

        assertEquals(1, projectRepository.findAllByWorkspaceId(workspace.id).size)
    }

    @Test
    fun `DELETE projects — happy, row 삭제 + ProjectDiscardedEvent 발행 + 204`(events: ApplicationEvents) {
        val (_, project) = seedWorkspaceAndProject(creatorUserId = 1L)

        mockMvc.delete("/projects/${project.id}").andExpect {
            status { isNoContent() }
        }

        assert(projectRepository.findById(project.id) == null)

        val discarded = events.stream(ProjectDiscardedEvent::class.java).toList()
        assertEquals(1, discarded.size)
        assertEquals(project.id, discarded.first().projectId)
    }

    @Test
    fun `DELETE projects — 호출자가 Project Admin 아님 → 403`() {
        val (_, project) = seedWorkspaceAndProject(creatorUserId = 1L)
        getCurrentUserId.userId = 999L

        mockMvc.delete("/projects/${project.id}").andExpect {
            status { isForbidden() }
        }

        assert(projectRepository.findById(project.id) != null)
    }

    @Test
    fun `DELETE projects — project 없음 → 404`() {
        mockMvc.delete("/projects/9999999").andExpect {
            status { isNotFound() }
        }
    }

    // ---- Step 2 — ProjectMember Assign/ChangeRole/Remove ----

    @Test
    fun `POST members — happy, ProjectMember row 생성 + 200`() {
        val (workspace, project) = seedWorkspaceAndProject(creatorUserId = 1L)
        val invitee = seedWorkspaceMember(workspaceId = workspace.id, userId = 2L)

        mockMvc.post("/projects/${project.id}/members") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                AssignMemberRequest(workspaceMemberId = invitee.id, role = ProjectMemberRole.MEMBER),
            )
        }.andExpect {
            status { isOk() }
        }

        val members = projectMemberRepository.findAllByProjectId(project.id)
        assertEquals(2, members.size) // 생성자 Admin + 신규 MEMBER
        val newMember = members.single { it.workspaceMemberId == invitee.id }
        assertEquals(ProjectMemberRole.MEMBER, newMember.role)
    }

    @Test
    fun `POST members — project 없음 → 404`() {
        mockMvc.post("/projects/9999999/members") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                AssignMemberRequest(workspaceMemberId = 1L, role = ProjectMemberRole.MEMBER),
            )
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `POST members — 호출자가 Project Admin 아님 → 403`() {
        val (workspace, project) = seedWorkspaceAndProject(creatorUserId = 1L)
        val invitee = seedWorkspaceMember(workspaceId = workspace.id, userId = 2L)
        getCurrentUserId.userId = 999L

        mockMvc.post("/projects/${project.id}/members") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                AssignMemberRequest(workspaceMemberId = invitee.id, role = ProjectMemberRole.MEMBER),
            )
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `POST members — workspace_member가 다른 워크스페이스 → 404`() {
        val (_, project) = seedWorkspaceAndProject(creatorUserId = 1L)
        val otherWorkspace = workspaceRepository.save(
            Workspace(
                notionWorkspaceId = "nw-other-${System.nanoTime()}",
                notionWorkspaceName = "Other",
                notionBotId = "bot-other",
                createdBy = 555L,
                createdAt = OffsetDateTime.now(),
            ),
        )
        val foreignMember = seedWorkspaceMember(workspaceId = otherWorkspace.id, userId = 2L)

        mockMvc.post("/projects/${project.id}/members") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                AssignMemberRequest(workspaceMemberId = foreignMember.id, role = ProjectMemberRole.MEMBER),
            )
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `POST members — 이미 ProjectMember → 409`() {
        val (workspace, project) = seedWorkspaceAndProject(creatorUserId = 1L)
        val invitee = seedWorkspaceMember(workspaceId = workspace.id, userId = 2L)
        projectMemberRepository.save(
            ProjectMember(projectId = project.id, workspaceMemberId = invitee.id, role = ProjectMemberRole.MEMBER),
        )

        mockMvc.post("/projects/${project.id}/members") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                AssignMemberRequest(workspaceMemberId = invitee.id, role = ProjectMemberRole.MEMBER),
            )
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `PATCH members — happy, role 변경 + 204`() {
        val (workspace, project) = seedWorkspaceAndProject(creatorUserId = 1L)
        val invitee = seedWorkspaceMember(workspaceId = workspace.id, userId = 2L)
        val member = projectMemberRepository.save(
            ProjectMember(projectId = project.id, workspaceMemberId = invitee.id, role = ProjectMemberRole.MEMBER),
        )

        mockMvc.patch("/projects/${project.id}/members/${member.id}") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateProjectMemberRoleRequest(role = ProjectMemberRole.ADMIN))
        }.andExpect {
            status { isNoContent() }
        }

        val updated = projectMemberRepository.findById(member.id)
        assertEquals(ProjectMemberRole.ADMIN, updated?.role)
    }

    @Test
    fun `PATCH members — project_member 없음 → 404`() {
        val (_, project) = seedWorkspaceAndProject(creatorUserId = 1L)

        mockMvc.patch("/projects/${project.id}/members/9999999") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateProjectMemberRoleRequest(role = ProjectMemberRole.ADMIN))
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `PATCH members — 호출자가 Project Admin 아님 → 403`() {
        val (workspace, project) = seedWorkspaceAndProject(creatorUserId = 1L)
        val invitee = seedWorkspaceMember(workspaceId = workspace.id, userId = 2L)
        val member = projectMemberRepository.save(
            ProjectMember(projectId = project.id, workspaceMemberId = invitee.id, role = ProjectMemberRole.MEMBER),
        )
        getCurrentUserId.userId = 999L

        mockMvc.patch("/projects/${project.id}/members/${member.id}") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateProjectMemberRoleRequest(role = ProjectMemberRole.ADMIN))
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `PATCH members — 자기 자신 role 변경 → 400`() {
        val (_, project) = seedWorkspaceAndProject(creatorUserId = 1L)
        val selfMember = projectMemberRepository.findAllByProjectId(project.id).first()

        mockMvc.patch("/projects/${project.id}/members/${selfMember.id}") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateProjectMemberRoleRequest(role = ProjectMemberRole.MEMBER))
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `DELETE members — happy, row 삭제 + 204`() {
        val (workspace, project) = seedWorkspaceAndProject(creatorUserId = 1L)
        val invitee = seedWorkspaceMember(workspaceId = workspace.id, userId = 2L)
        val member = projectMemberRepository.save(
            ProjectMember(projectId = project.id, workspaceMemberId = invitee.id, role = ProjectMemberRole.MEMBER),
        )

        mockMvc.delete("/projects/${project.id}/members/${member.id}").andExpect {
            status { isNoContent() }
        }

        assert(projectMemberRepository.findById(member.id) == null)
    }

    @Test
    fun `DELETE members — project_member 없음 → 404`() {
        val (_, project) = seedWorkspaceAndProject(creatorUserId = 1L)

        mockMvc.delete("/projects/${project.id}/members/9999999").andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `DELETE members — 호출자가 Project Admin 아님 → 403`() {
        val (workspace, project) = seedWorkspaceAndProject(creatorUserId = 1L)
        val invitee = seedWorkspaceMember(workspaceId = workspace.id, userId = 2L)
        val member = projectMemberRepository.save(
            ProjectMember(projectId = project.id, workspaceMemberId = invitee.id, role = ProjectMemberRole.MEMBER),
        )
        getCurrentUserId.userId = 999L

        mockMvc.delete("/projects/${project.id}/members/${member.id}").andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `DELETE members — 자기 자신 제거 → 400`() {
        val (_, project) = seedWorkspaceAndProject(creatorUserId = 1L)
        val selfMember = projectMemberRepository.findAllByProjectId(project.id).first()

        mockMvc.delete("/projects/${project.id}/members/${selfMember.id}").andExpect {
            status { isBadRequest() }
        }
    }

    // ---- Step 3 — Category Register/Remove/ChangeType ----

    @Test
    fun `POST categories — happy, Category row 생성 + 200`() {
        val (_, project) = seedWorkspaceAndProject(creatorUserId = 1L)

        mockMvc.post("/projects/${project.id}/categories") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                RegisterCategoryRequest(notionPageId = "page-meeting", documentType = DocumentType.MEETING_NOTES),
            )
        }.andExpect {
            status { isOk() }
        }

        val categories = categoryRepository.findAllByProjectId(project.id)
        assertEquals(1, categories.size)
        val category = categories.first()
        assertEquals("page-meeting", category.notionPageId)
        assertEquals(DocumentType.MEETING_NOTES, category.documentType)
    }

    @Test
    fun `POST categories — project 없음 → 404`() {
        mockMvc.post("/projects/9999999/categories") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                RegisterCategoryRequest(notionPageId = "page-x", documentType = DocumentType.PLANNING),
            )
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `POST categories — 호출자가 Project Admin 아님 → 403`() {
        val (_, project) = seedWorkspaceAndProject(creatorUserId = 1L)
        getCurrentUserId.userId = 999L

        mockMvc.post("/projects/${project.id}/categories") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                RegisterCategoryRequest(notionPageId = "page-x", documentType = DocumentType.PLANNING),
            )
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `POST categories — 같은 notion_page_id 중복 → 409`() {
        val (_, project) = seedWorkspaceAndProject(creatorUserId = 1L)
        categoryRepository.save(
            Category(projectId = project.id, notionPageId = "page-dup", documentType = DocumentType.MEETING_NOTES),
        )

        mockMvc.post("/projects/${project.id}/categories") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                RegisterCategoryRequest(notionPageId = "page-dup", documentType = DocumentType.PLANNING),
            )
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `PATCH categories — happy, type 변경 + 204`() {
        val (_, project) = seedWorkspaceAndProject(creatorUserId = 1L)
        val category = categoryRepository.save(
            Category(projectId = project.id, notionPageId = "page-x", documentType = DocumentType.MEETING_NOTES),
        )

        mockMvc.patch("/projects/${project.id}/categories/${category.id}") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(ChangeCategoryTypeRequest(documentType = DocumentType.PLANNING))
        }.andExpect {
            status { isNoContent() }
        }

        val updated = categoryRepository.findById(category.id)
        assertEquals(DocumentType.PLANNING, updated?.documentType)
    }

    @Test
    fun `PATCH categories — category 없음 → 404`() {
        val (_, project) = seedWorkspaceAndProject(creatorUserId = 1L)

        mockMvc.patch("/projects/${project.id}/categories/9999999") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(ChangeCategoryTypeRequest(documentType = DocumentType.PLANNING))
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `PATCH categories — category가 다른 project → 404`() {
        val (_, projectA) = seedWorkspaceAndProject(creatorUserId = 1L)
        val (_, projectB) = seedWorkspaceAndProject(creatorUserId = 1L)
        val categoryInA = categoryRepository.save(
            Category(projectId = projectA.id, notionPageId = "page-a", documentType = DocumentType.MEETING_NOTES),
        )

        mockMvc.patch("/projects/${projectB.id}/categories/${categoryInA.id}") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(ChangeCategoryTypeRequest(documentType = DocumentType.PLANNING))
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `PATCH categories — 호출자가 Project Admin 아님 → 403`() {
        val (_, project) = seedWorkspaceAndProject(creatorUserId = 1L)
        val category = categoryRepository.save(
            Category(projectId = project.id, notionPageId = "page-x", documentType = DocumentType.MEETING_NOTES),
        )
        getCurrentUserId.userId = 999L

        mockMvc.patch("/projects/${project.id}/categories/${category.id}") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(ChangeCategoryTypeRequest(documentType = DocumentType.PLANNING))
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `DELETE categories — happy, row 삭제 + 204`() {
        val (_, project) = seedWorkspaceAndProject(creatorUserId = 1L)
        val category = categoryRepository.save(
            Category(projectId = project.id, notionPageId = "page-x", documentType = DocumentType.MEETING_NOTES),
        )

        mockMvc.delete("/projects/${project.id}/categories/${category.id}").andExpect {
            status { isNoContent() }
        }

        assert(categoryRepository.findById(category.id) == null)
    }

    @Test
    fun `DELETE categories — category 없음 → 404`() {
        val (_, project) = seedWorkspaceAndProject(creatorUserId = 1L)

        mockMvc.delete("/projects/${project.id}/categories/9999999").andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `DELETE categories — 호출자가 Project Admin 아님 → 403`() {
        val (_, project) = seedWorkspaceAndProject(creatorUserId = 1L)
        val category = categoryRepository.save(
            Category(projectId = project.id, notionPageId = "page-x", documentType = DocumentType.MEETING_NOTES),
        )
        getCurrentUserId.userId = 999L

        mockMvc.delete("/projects/${project.id}/categories/${category.id}").andExpect {
            status { isForbidden() }
        }
    }

    // ---- Step 4 — TypeAssigneeDefault Configure ----

    @Test
    fun `PUT type-assignees — happy, 기존 row 대체 + 부분 list + null 슬롯 + 204`() {
        val (workspace, project) = seedWorkspaceAndProject(creatorUserId = 1L)
        val member = seedWorkspaceMember(workspaceId = workspace.id, userId = 2L)
        // 기존 row 박음 — replace로 사라져야 함
        typeAssigneeRepository.save(
            TypeAssigneeDefault(
                projectId = project.id,
                documentType = DocumentType.MEETING_NOTES,
                assigneeWorkspaceMemberId = null,
            ),
        )

        mockMvc.put("/projects/${project.id}/type-assignees") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                UpdateTypeAssigneesRequest(
                    assignees = listOf(
                        TypeAssigneeItem(DocumentType.PLANNING, assigneeMemberId = member.id),
                        TypeAssigneeItem(DocumentType.REQUIREMENTS, assigneeMemberId = null),
                    ),
                ),
            )
        }.andExpect {
            status { isNoContent() }
        }

        val rows = typeAssigneeRepository.findAllByProjectId(project.id)
        assertEquals(2, rows.size)
        val planning = rows.single { it.documentType == DocumentType.PLANNING }
        assertEquals(member.id, planning.assigneeWorkspaceMemberId)
        val requirements = rows.single { it.documentType == DocumentType.REQUIREMENTS }
        assertEquals(null, requirements.assigneeWorkspaceMemberId)
        assert(rows.none { it.documentType == DocumentType.MEETING_NOTES }) // 기존 row 사라짐
    }

    @Test
    fun `PUT type-assignees — 동일 payload 재호출 idempotent + 204`() {
        val (workspace, project) = seedWorkspaceAndProject(creatorUserId = 1L)
        val member = seedWorkspaceMember(workspaceId = workspace.id, userId = 2L)
        val body = objectMapper.writeValueAsString(
            UpdateTypeAssigneesRequest(
                assignees = listOf(
                    TypeAssigneeItem(DocumentType.PLANNING, assigneeMemberId = member.id),
                    TypeAssigneeItem(DocumentType.REQUIREMENTS, assigneeMemberId = null),
                ),
            ),
        )

        // 동일 (project_id, document_type) 재제출 시 기존 row가 먼저 삭제되어 unique 충돌 없이 대체돼야 함
        repeat(2) {
            mockMvc.put("/projects/${project.id}/type-assignees") {
                contentType = MediaType.APPLICATION_JSON
                content = body
            }.andExpect {
                status { isNoContent() }
            }
        }

        assertEquals(2, typeAssigneeRepository.findAllByProjectId(project.id).size)
    }

    @Test
    fun `PUT type-assignees — 빈 list, 모든 row 삭제 + 204`() {
        val (_, project) = seedWorkspaceAndProject(creatorUserId = 1L)
        typeAssigneeRepository.save(
            TypeAssigneeDefault(
                projectId = project.id,
                documentType = DocumentType.MEETING_NOTES,
                assigneeWorkspaceMemberId = null,
            ),
        )

        mockMvc.put("/projects/${project.id}/type-assignees") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateTypeAssigneesRequest(assignees = emptyList()))
        }.andExpect {
            status { isNoContent() }
        }

        assertEquals(0, typeAssigneeRepository.findAllByProjectId(project.id).size)
    }

    @Test
    fun `PUT type-assignees — project 없음 → 404`() {
        mockMvc.put("/projects/9999999/type-assignees") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateTypeAssigneesRequest(assignees = emptyList()))
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `PUT type-assignees — 호출자가 Project Admin 아님 → 403`() {
        val (_, project) = seedWorkspaceAndProject(creatorUserId = 1L)
        getCurrentUserId.userId = 999L

        mockMvc.put("/projects/${project.id}/type-assignees") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateTypeAssigneesRequest(assignees = emptyList()))
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `PUT type-assignees — assignee가 다른 워크스페이스 → 404`() {
        val (_, project) = seedWorkspaceAndProject(creatorUserId = 1L)
        val otherWorkspace = workspaceRepository.save(
            Workspace(
                notionWorkspaceId = "nw-other-${System.nanoTime()}",
                notionWorkspaceName = "Other",
                notionBotId = "bot-other",
                createdBy = 555L,
                createdAt = OffsetDateTime.now(),
            ),
        )
        val foreignMember = seedWorkspaceMember(workspaceId = otherWorkspace.id, userId = 2L)

        mockMvc.put("/projects/${project.id}/type-assignees") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                UpdateTypeAssigneesRequest(
                    assignees = listOf(TypeAssigneeItem(DocumentType.PLANNING, assigneeMemberId = foreignMember.id)),
                ),
            )
        }.andExpect {
            status { isNotFound() }
        }
    }

    // ---- Step 5 — Sync trigger ----

    @Test
    fun `POST sync — happy, ProjectSyncTriggeredEvent 발행 + 204`(events: ApplicationEvents) {
        val (_, project) = seedWorkspaceAndProject(creatorUserId = 1L)

        mockMvc.post("/projects/${project.id}/sync").andExpect {
            status { isNoContent() }
        }

        val triggered = events.stream(ProjectSyncTriggeredEvent::class.java).toList()
        assertEquals(1, triggered.size)
        assertEquals(project.id, triggered.first().projectId)
        assertEquals(1L, triggered.first().triggeredBy)
    }

    @Test
    fun `POST sync — project 없음 → 404`() {
        mockMvc.post("/projects/9999999/sync").andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `POST sync — 호출자가 Project Admin 아님 → 403`() {
        val (_, project) = seedWorkspaceAndProject(creatorUserId = 1L)
        getCurrentUserId.userId = 999L

        mockMvc.post("/projects/${project.id}/sync").andExpect {
            status { isForbidden() }
        }
    }

    // ---- helpers ----

    private fun seedWorkspaceWithCreator(creatorUserId: Long): Pair<Workspace, WorkspaceMember> {
        val workspace = workspaceRepository.save(
            Workspace(
                notionWorkspaceId = "nw-proj-${System.nanoTime()}",
                notionWorkspaceName = "Test Workspace",
                notionBotId = "bot-test",
                createdBy = creatorUserId,
                createdAt = OffsetDateTime.now(),
            ),
        )
        val creatorMember = workspaceMemberRepository.save(
            WorkspaceMember(workspaceId = workspace.id, userId = creatorUserId),
        )
        return workspace to creatorMember
    }

    private fun seedWorkspaceAndProject(creatorUserId: Long): Pair<Workspace, Project> {
        val (workspace, creatorMember) = seedWorkspaceWithCreator(creatorUserId)
        val project = projectRepository.save(
            Project(
                workspaceId = workspace.id,
                name = "Seed Project",
                notionRootPageId = "root-${System.nanoTime()}",
                createdBy = creatorUserId,
            ),
        )
        projectMemberRepository.save(
            ProjectMember(projectId = project.id, workspaceMemberId = creatorMember.id, role = ProjectMemberRole.ADMIN),
        )
        return workspace to project
    }

    private fun seedWorkspaceMember(workspaceId: Long, userId: Long): WorkspaceMember =
        workspaceMemberRepository.save(WorkspaceMember(workspaceId = workspaceId, userId = userId))
}
