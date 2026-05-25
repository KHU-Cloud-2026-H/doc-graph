package com.docgraph.backend.workspace.query.interfaces.api

import com.docgraph.backend.auth.query.application.GetCurrentUserIdQuery
import com.docgraph.backend.auth.query.application.SearchUserAccountsByIdsQuery
import com.docgraph.backend.auth.query.application.UserResponse
import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.workspace.command.domain.Workspace
import com.docgraph.backend.workspace.command.domain.WorkspaceMember
import com.docgraph.backend.workspace.command.domain.WorkspaceMemberRepository
import com.docgraph.backend.workspace.command.domain.WorkspaceRepository
import org.hamcrest.Matchers.greaterThanOrEqualTo
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

class FakeGetCurrentUserIdQuery : GetCurrentUserIdQuery {
    @Volatile var userId: Long = 1L
    override fun get(): Long = userId
}

class FakeSearchUserAccountsByIdsQuery : SearchUserAccountsByIdsQuery {
    @Volatile var behavior: (List<Long>) -> List<UserResponse> = { ids ->
        ids.map { UserResponse(it, "user-$it@example.com", "User $it", null) }
    }
    override fun search(userIds: List<Long>): List<UserResponse> = behavior(userIds)
}

@TestConfiguration
class WorkspaceQueryControllerTestConfig {
    @Bean @Primary fun fakeGetCurrentUserId() = FakeGetCurrentUserIdQuery()
    @Bean @Primary fun fakeSearchUserAccountsByIds() = FakeSearchUserAccountsByIdsQuery()
}

@Tag("component")
@SpringBootTest
@AutoConfigureMockMvc
@Import(WorkspaceQueryControllerTestConfig::class, SharedPostgresContainer::class)
class WorkspaceQueryControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val workspaceRepository: WorkspaceRepository,
    private val memberRepository: WorkspaceMemberRepository,
    private val getCurrentUserId: FakeGetCurrentUserIdQuery,
) {

    @BeforeEach
    fun reset() {
        getCurrentUserId.userId = 1L
    }

    @Test
    fun `GET workspaces — 소유 + 참여 반환, 비참여 제외`() {
        getCurrentUserId.userId = 1001L
        val owned = seedWorkspace(createdBy = 1001L)
        val joined = seedWorkspace(createdBy = 9999L)
        memberRepository.save(WorkspaceMember(workspaceId = joined.id, userId = 1001L))
        val notMine = seedWorkspace(createdBy = 9999L)

        mockMvc.get("/workspaces").andExpect {
            status { isOk() }
            jsonPath("$[?(@.id == ${owned.id})].name") { value("Test Workspace") }
            jsonPath("$[?(@.id == ${joined.id})].name") { value("Test Workspace") }
            jsonPath("$[?(@.id == ${notMine.id})]") { isEmpty() }
            jsonPath("$[?(@.id == ${owned.id})].projectCount") { value(0) }
            jsonPath("$[?(@.id == ${owned.id})].documentCount") { value(0) }
            jsonPath("$[?(@.id == ${owned.id})].unresolvedConflictCount") { value(0) }
        }
    }

    @Test
    fun `GET workspaces id — 소유 → 200 + 상세`() {
        getCurrentUserId.userId = 2001L
        val workspace = seedWorkspace(createdBy = 2001L)

        mockMvc.get("/workspaces/${workspace.id}").andExpect {
            status { isOk() }
            jsonPath("$.id") { value(workspace.id) }
            jsonPath("$.name") { value("Test Workspace") }
            jsonPath("$.members.length()") { value(0) }
            jsonPath("$.memberCount") { value(0) }
            jsonPath("$.projectCount") { value(0) }
            jsonPath("$.documentCount") { value(0) }
            jsonPath("$.unresolvedConflictCount") { value(0) }
        }
    }

    @Test
    fun `GET workspaces id — 멤버 row → members 응답에 user account 결합`() {
        getCurrentUserId.userId = 3001L
        val workspace = seedWorkspace(createdBy = 3001L)
        memberRepository.save(WorkspaceMember(workspaceId = workspace.id, userId = 3002L))
        memberRepository.save(WorkspaceMember(workspaceId = workspace.id, userId = 3003L))

        mockMvc.get("/workspaces/${workspace.id}").andExpect {
            status { isOk() }
            jsonPath("$.members.length()") { value(2) }
            jsonPath("$.members[?(@.userId == 3002)].name") { value("User 3002") }
            jsonPath("$.members[?(@.userId == 3002)].email") { value("user-3002@example.com") }
            jsonPath("$.members[?(@.userId == 3003)].name") { value("User 3003") }
        }
    }

    @Test
    fun `GET workspaces id — 권한 없음 → 404`() {
        getCurrentUserId.userId = 4001L
        val workspace = seedWorkspace(createdBy = 9999L)

        mockMvc.get("/workspaces/${workspace.id}").andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `GET workspaces id — 워크스페이스 자체 없음 → 404`() {
        mockMvc.get("/workspaces/99999999").andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `GET workspaces — 어떤 워크스페이스도 없는 user → 빈 배열`() {
        getCurrentUserId.userId = 5001L

        mockMvc.get("/workspaces").andExpect {
            status { isOk() }
            jsonPath("$") { isArray() }
        }
    }

    private fun seedWorkspace(createdBy: Long): Workspace =
        workspaceRepository.save(
            Workspace(
                notionWorkspaceId = "nw-query-${System.nanoTime()}",
                notionWorkspaceName = "Test Workspace",
                notionBotId = "bot-test",
                createdBy = createdBy,
                createdAt = OffsetDateTime.now(),
            ),
        )
}