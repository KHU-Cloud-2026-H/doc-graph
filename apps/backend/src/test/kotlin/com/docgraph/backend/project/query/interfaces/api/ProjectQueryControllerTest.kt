package com.docgraph.backend.project.query.interfaces.api

import com.docgraph.backend.auth.query.application.GetCurrentUserIdQuery
import com.docgraph.backend.auth.query.application.SearchUserAccountsByIdsQuery
import com.docgraph.backend.auth.query.application.UserResponse
import com.docgraph.backend.document.query.application.DocumentType
import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.project.command.domain.Category
import com.docgraph.backend.project.command.domain.CategoryRepository
import com.docgraph.backend.project.command.domain.Project
import com.docgraph.backend.project.command.domain.ProjectMember
import com.docgraph.backend.project.command.domain.ProjectMemberRepository
import com.docgraph.backend.project.command.domain.ProjectMemberRole
import com.docgraph.backend.project.command.domain.ProjectRepository
import com.docgraph.backend.project.command.domain.TypeAssigneeDefault
import com.docgraph.backend.project.command.domain.TypeAssigneeDefaultRepository
import com.docgraph.backend.workspace.command.domain.Workspace
import com.docgraph.backend.workspace.command.domain.WorkspaceMember
import com.docgraph.backend.workspace.command.domain.WorkspaceMemberRepository
import com.docgraph.backend.workspace.command.domain.WorkspaceRepository
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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.OffsetDateTime
import kotlin.test.assertEquals

class FakeGetCurrentUserIdQueryQ : GetCurrentUserIdQuery {
    @Volatile var userId: Long = 1L
    override fun get(): Long = userId
}

class FakeSearchUserAccountsByIdsQueryQ : SearchUserAccountsByIdsQuery {
    @Volatile var behavior: (List<Long>) -> List<UserResponse> = { ids ->
        ids.map { UserResponse(id = it, email = "user-$it@example.com", name = "User $it") }
    }
    override fun search(userIds: List<Long>): List<UserResponse> = behavior(userIds)
}

@TestConfiguration
class ProjectQueryControllerTestConfig {
    @Bean @Primary fun fakeGetCurrentUserId() = FakeGetCurrentUserIdQueryQ()
    @Bean @Primary fun fakeSearchUserAccountsByIds() = FakeSearchUserAccountsByIdsQueryQ()
}

@Tag("component")
@SpringBootTest
@AutoConfigureMockMvc
@Import(ProjectQueryControllerTestConfig::class, SharedPostgresContainer::class)
class ProjectQueryControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val workspaceRepository: WorkspaceRepository,
    private val workspaceMemberRepository: WorkspaceMemberRepository,
    private val projectRepository: ProjectRepository,
    private val projectMemberRepository: ProjectMemberRepository,
    private val categoryRepository: CategoryRepository,
    private val typeAssigneeRepository: TypeAssigneeDefaultRepository,
    private val getCurrentUserId: FakeGetCurrentUserIdQueryQ,
) {

    @BeforeEach
    fun reset() {
        getCurrentUserId.userId = 1L
    }

    @Test
    fun `GET workspaces projects — happy, 본인 접근 가능한 project만`() {
        val (workspace, _) = seedWorkspaceWithCreator(creatorUserId = 1L)
        val pA = seedProjectWithAdmin(workspaceId = workspace.id, adminUserId = 1L, name = "A")
        val pB = seedProjectWithAdmin(workspaceId = workspace.id, adminUserId = 1L, name = "B")
        // 본인이 멤버가 아닌 다른 user의 project — 응답에 없어야 함
        val (otherWs, _) = seedWorkspaceWithCreator(creatorUserId = 555L)
        seedProjectWithAdmin(workspaceId = otherWs.id, adminUserId = 555L, name = "Other")

        mockMvc.get("/workspaces/${workspace.id}/projects").andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }
            jsonPath("$[?(@.id == ${pA.id})].name") { value("A") }
            jsonPath("$[?(@.id == ${pB.id})].name") { value("B") }
        }
    }

    @Test
    fun `GET workspaces projects — 본인이 워크스페이스 비멤버 → 빈 list`() {
        val (workspace, _) = seedWorkspaceWithCreator(creatorUserId = 555L)
        seedProjectWithAdmin(workspaceId = workspace.id, adminUserId = 555L, name = "X")

        mockMvc.get("/workspaces/${workspace.id}/projects").andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(0) }
        }
    }

    @Test
    fun `GET projects detail — happy, ProjectDetail + members 반환`() {
        val (workspace, _) = seedWorkspaceWithCreator(creatorUserId = 1L)
        val project = seedProjectWithAdmin(workspaceId = workspace.id, adminUserId = 1L, name = "Q2")

        mockMvc.get("/projects/${project.id}").andExpect {
            status { isOk() }
            jsonPath("$.id") { value(project.id) }
            jsonPath("$.name") { value("Q2") }
            jsonPath("$.notionRootPageId") { exists() }
            jsonPath("$.members.length()") { value(1) }
            jsonPath("$.members[0].role") { value(ProjectMemberRole.ADMIN.name) }
            jsonPath("$.members[0].name") { value("User 1") }
        }
    }

    @Test
    fun `GET projects detail — project 없음 → 404`() {
        mockMvc.get("/projects/9999999").andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `GET projects detail — 본인 비멤버 → 404`() {
        val (workspace, _) = seedWorkspaceWithCreator(creatorUserId = 555L)
        val project = seedProjectWithAdmin(workspaceId = workspace.id, adminUserId = 555L, name = "X")

        mockMvc.get("/projects/${project.id}").andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `GET projects categories — happy, list 반환`() {
        val (workspace, _) = seedWorkspaceWithCreator(creatorUserId = 1L)
        val project = seedProjectWithAdmin(workspaceId = workspace.id, adminUserId = 1L, name = "P")
        categoryRepository.save(
            Category(projectId = project.id, notionPageId = "page-mn", documentType = DocumentType.MEETING_NOTES),
        )
        categoryRepository.save(
            Category(projectId = project.id, notionPageId = "page-pl", documentType = DocumentType.PLANNING),
        )

        mockMvc.get("/projects/${project.id}/categories").andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }
            jsonPath("$[?(@.notionPageId == 'page-mn')].documentType") { value("meeting_notes") }
            jsonPath("$[?(@.notionPageId == 'page-pl')].documentType") { value("planning") }
        }
    }

    @Test
    fun `GET projects categories — 본인 비멤버 → 404`() {
        val (workspace, _) = seedWorkspaceWithCreator(creatorUserId = 555L)
        val project = seedProjectWithAdmin(workspaceId = workspace.id, adminUserId = 555L, name = "X")

        mockMvc.get("/projects/${project.id}/categories").andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `GET projects type-assignees — happy, list 반환`() {
        val (workspace, creatorWsMember) = seedWorkspaceWithCreator(creatorUserId = 1L)
        val project = seedProjectWithAdmin(workspaceId = workspace.id, adminUserId = 1L, name = "P")
        typeAssigneeRepository.save(
            TypeAssigneeDefault(
                projectId = project.id,
                documentType = DocumentType.MEETING_NOTES,
                assigneeWorkspaceMemberId = creatorWsMember.id,
            ),
        )
        typeAssigneeRepository.save(
            TypeAssigneeDefault(
                projectId = project.id,
                documentType = DocumentType.PLANNING,
                assigneeWorkspaceMemberId = null,
            ),
        )

        mockMvc.get("/projects/${project.id}/type-assignees").andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }
            jsonPath("$[?(@.documentType == 'meeting_notes')].assigneeMemberId") { value(creatorWsMember.id.toInt()) }
            jsonPath("$[?(@.documentType == 'planning')].assigneeMemberId") { value(null) }
        }
    }

    @Test
    fun `GET projects type-assignees — 본인 비멤버 → 404`() {
        val (workspace, _) = seedWorkspaceWithCreator(creatorUserId = 555L)
        val project = seedProjectWithAdmin(workspaceId = workspace.id, adminUserId = 555L, name = "X")

        mockMvc.get("/projects/${project.id}/type-assignees").andExpect {
            status { isNotFound() }
        }
    }

    // ---- helpers ----

    private fun seedWorkspaceWithCreator(creatorUserId: Long): Pair<Workspace, WorkspaceMember> {
        val workspace = workspaceRepository.save(
            Workspace(
                notionWorkspaceId = "nw-q-${System.nanoTime()}",
                notionWorkspaceName = "WS",
                notionBotId = "bot",
                createdBy = creatorUserId,
                createdAt = OffsetDateTime.now(),
            ),
        )
        val member = workspaceMemberRepository.save(
            WorkspaceMember(workspaceId = workspace.id, userId = creatorUserId),
        )
        return workspace to member
    }

    private fun seedProjectWithAdmin(workspaceId: Long, adminUserId: Long, name: String): Project {
        val wsMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, adminUserId)!!
        val project = projectRepository.save(
            Project(
                workspaceId = workspaceId,
                name = name,
                notionRootPageId = "root-${System.nanoTime()}",
                createdBy = adminUserId,
            ),
        )
        projectMemberRepository.save(
            ProjectMember(projectId = project.id, workspaceMemberId = wsMember.id, role = ProjectMemberRole.ADMIN),
        )
        return project
    }
}
