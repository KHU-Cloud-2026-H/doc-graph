package com.docgraph.backend.workspace.query.infra

import com.docgraph.backend.fixtures.SharedPostgresContainer
import com.docgraph.backend.workspace.command.domain.Workspace
import com.docgraph.backend.workspace.command.domain.WorkspaceMember
import com.docgraph.backend.workspace.command.infra.WorkspaceJpaRepository
import com.docgraph.backend.workspace.command.infra.WorkspaceMemberJpaRepository
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Tag("slice")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SharedPostgresContainer::class, WorkspaceQueryRepository::class)
class WorkspaceQueryRepositoryTest @Autowired constructor(
    private val queryRepository: WorkspaceQueryRepository,
    private val workspaceJpa: WorkspaceJpaRepository,
    private val workspaceMemberJpa: WorkspaceMemberJpaRepository,
) {

    @Test
    fun `findMemberIdsByUserId — user가 가입한 workspace_member id 묶음 반환`() {
        val ws = workspaceJpa.save(newWorkspace("ws-1"))
        val member = workspaceMemberJpa.save(WorkspaceMember(workspaceId = ws.id, userId = 100L))

        val ids = queryRepository.findMemberIdsByUserId(100L)

        assertEquals(listOf(member.id), ids)
    }

    @Test
    fun `findMemberIdsByUserId — 한 user가 여러 workspace 가입 시 모든 member id`() {
        val ws1 = workspaceJpa.save(newWorkspace("ws-a"))
        val ws2 = workspaceJpa.save(newWorkspace("ws-b"))
        val m1 = workspaceMemberJpa.save(WorkspaceMember(workspaceId = ws1.id, userId = 100L))
        val m2 = workspaceMemberJpa.save(WorkspaceMember(workspaceId = ws2.id, userId = 100L))

        val ids = queryRepository.findMemberIdsByUserId(100L).toSet()

        assertEquals(setOf(m1.id, m2.id), ids)
    }

    @Test
    fun `findMemberIdsByUserId — 다른 user 매핑은 제외`() {
        val ws = workspaceJpa.save(newWorkspace("ws-2"))
        workspaceMemberJpa.save(WorkspaceMember(workspaceId = ws.id, userId = 200L))

        val ids = queryRepository.findMemberIdsByUserId(100L)

        assertTrue(ids.isEmpty())
    }

    @Test
    fun `findMemberIdsByUserId — 가입 없으면 빈 리스트`() {
        val ids = queryRepository.findMemberIdsByUserId(99999L)
        assertTrue(ids.isEmpty())
    }

    private fun newWorkspace(notionId: String) = Workspace(
        notionWorkspaceId = notionId,
        notionWorkspaceName = notionId,
        notionBotId = "bot-$notionId",
        createdBy = 1L,
    )
}
