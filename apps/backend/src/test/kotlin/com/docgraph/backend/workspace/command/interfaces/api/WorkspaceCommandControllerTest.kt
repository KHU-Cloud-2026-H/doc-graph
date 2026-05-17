package com.docgraph.backend.workspace.command.interfaces.api

import com.docgraph.backend.auth.query.application.FindUserIdByEmailQuery
import com.docgraph.backend.auth.query.application.GetCurrentUserIdQuery
import com.docgraph.backend.fixtures.SharedPostgresContainer
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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.post
import java.time.OffsetDateTime

class FakeGetCurrentUserIdQuery : GetCurrentUserIdQuery {
    @Volatile var userId: Long = 1L
    override fun get(): Long = userId
}

class FakeFindUserIdByEmailQuery : FindUserIdByEmailQuery {
    @Volatile var behavior: (String) -> Long? = { null }
    override fun find(email: String): Long? = behavior(email)
}

@TestConfiguration
class WorkspaceCommandControllerTestConfig {
    @Bean @Primary fun fakeGetCurrentUserId() = FakeGetCurrentUserIdQuery()
    @Bean @Primary fun fakeFindUserIdByEmail() = FakeFindUserIdByEmailQuery()
}

@Tag("component")
@SpringBootTest
@AutoConfigureMockMvc
@Import(WorkspaceCommandControllerTestConfig::class, SharedPostgresContainer::class)
class WorkspaceCommandControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val workspaceRepository: WorkspaceRepository,
    private val memberRepository: WorkspaceMemberRepository,
    private val getCurrentUserId: FakeGetCurrentUserIdQuery,
    private val findUserIdByEmail: FakeFindUserIdByEmailQuery,
) {

    @BeforeEach
    fun reset() {
        getCurrentUserId.userId = 1L
        findUserIdByEmail.behavior = { null }
    }

    @Test
    fun `POST members — happy, row 생성 + 200`() {
        val workspace = seedWorkspace(createdBy = 1L)
        findUserIdByEmail.behavior = { if (it == "invitee@example.com") 200L else null }

        mockMvc.post("/workspaces/${workspace.id}/members") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(InviteMemberRequest("invitee@example.com"))
        }.andExpect {
            status { isOk() }
        }

        assert(memberRepository.findByWorkspaceIdAndUserId(workspace.id, 200L) != null)
    }

    @Test
    fun `POST members — workspace 없음 → 404`() {
        mockMvc.post("/workspaces/9999999/members") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(InviteMemberRequest("x@example.com"))
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `POST members — 호출자가 생성자 아님 → 403`() {
        val workspace = seedWorkspace(createdBy = 1L)
        getCurrentUserId.userId = 999L
        findUserIdByEmail.behavior = { 200L }

        mockMvc.post("/workspaces/${workspace.id}/members") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(InviteMemberRequest("invitee@example.com"))
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `POST members — invitee 이메일 미가입 → 404`() {
        val workspace = seedWorkspace(createdBy = 1L)
        findUserIdByEmail.behavior = { null }

        mockMvc.post("/workspaces/${workspace.id}/members") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(InviteMemberRequest("missing@example.com"))
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `POST members — 생성자 자기 초대 → 409`() {
        val workspace = seedWorkspace(createdBy = 1L)
        findUserIdByEmail.behavior = { 1L }

        mockMvc.post("/workspaces/${workspace.id}/members") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(InviteMemberRequest("creator@example.com"))
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `POST members — 중복 초대 → 409`() {
        val workspace = seedWorkspace(createdBy = 1L)
        memberRepository.save(WorkspaceMember(workspaceId = workspace.id, userId = 300L))
        findUserIdByEmail.behavior = { 300L }

        mockMvc.post("/workspaces/${workspace.id}/members") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(InviteMemberRequest("dup@example.com"))
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `DELETE members — happy, row 삭제 + 204`() {
        val workspace = seedWorkspace(createdBy = 1L)
        memberRepository.save(WorkspaceMember(workspaceId = workspace.id, userId = 400L))

        mockMvc.delete("/workspaces/${workspace.id}/members/400").andExpect {
            status { isNoContent() }
        }

        assert(memberRepository.findByWorkspaceIdAndUserId(workspace.id, 400L) == null)
    }

    @Test
    fun `DELETE members — 호출자가 생성자 아님 → 403`() {
        val workspace = seedWorkspace(createdBy = 1L)
        memberRepository.save(WorkspaceMember(workspaceId = workspace.id, userId = 500L))
        getCurrentUserId.userId = 999L

        mockMvc.delete("/workspaces/${workspace.id}/members/500").andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `DELETE members — 생성자 본인 제거 시도 → 400`() {
        val workspace = seedWorkspace(createdBy = 1L)

        mockMvc.delete("/workspaces/${workspace.id}/members/1").andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `DELETE members — 존재하지 않는 멤버 → 404`() {
        val workspace = seedWorkspace(createdBy = 1L)

        mockMvc.delete("/workspaces/${workspace.id}/members/9999").andExpect {
            status { isNotFound() }
        }
    }

    private fun seedWorkspace(createdBy: Long): Workspace =
        workspaceRepository.save(
            Workspace(
                notionWorkspaceId = "nw-cmd-${System.nanoTime()}",
                notionWorkspaceName = "Test Workspace",
                notionBotId = "bot-test",
                createdBy = createdBy,
                createdAt = OffsetDateTime.now(),
            ),
        )
}